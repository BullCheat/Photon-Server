package org.mcphoton.server

import better.files.File
import org.fusesource.jansi.AnsiConsole
import org.mcphoton.GameRegistry
import org.mcphoton.command.{CommandSystem, StopCommand}
import org.mcphoton.entity.mobs.Player
import org.mcphoton.network.ProtocolLibAdapter
import org.mcphoton.plugin._
import org.mcphoton.world.{World, WorldType}
import org.slf4j.LoggerFactory
import org.slf4j.impl.LoggingService

import scala.collection.mutable
import scala.util.{Failure, Success}
import util.control.Breaks._

/**
 * @author TheElectronWill
 */
object PhotonServer {
	// Logger
	private[mcphoton] val logger = LoggerFactory.getLogger("PhotonServer")

	// Constant infos
	final val Version: String = "0.5-alpha"
	final val MinecraftVersion: String = "1.11.2"

	// Directories
	final val DirMain = File(System.getProperty("user.dir"))
	final val DirConfig: File = DirMain / "config"
	final val DirPlugins: File = DirMain / "plugins"
	final val DirWorlds: File = DirMain / "worlds"
	final val DirLogs: File = DirMain / "logs"
	final val Config: ServerConfig = new ServerConfig
	final val WorldPluginsConfig: WorldPluginsConfig = new WorldPluginsConfig

	// Worlds
	private[this] val worldsNameMap = new mutable.AnyRefMap[String, World]

	private var _worldsCache: List[World] = worldsNameMap.values.toList
	def enabledWorlds: List[World] = _worldsCache


	def world(name: String): Option[World] = worldsNameMap.get(name)

	private[mcphoton] def registerWorld(w: World): Unit = {
		worldsNameMap.put(w.name, w)
		_worldsCache = worldsNameMap.values.toList
	}
	private[mcphoton] def unregisterWorld(w: World): Unit = {
		worldsNameMap.remove(w.name)
		_worldsCache = worldsNameMap.values.toList
	}

	// ProtocolLib
	private[this] var protocolLibAdapter: ProtocolLibAdapter = _

	// Players
	val onlinePlayers = new mutable.ArrayBuffer[Player]

	def main(args: Array[String]): Unit = {
		logger.info(s"Photon Server version $Version")
		loadDirs()
		AnsiConsole.systemInstall()

		logger.info("Loading the access controller")
		AccessController.load()

		logger.info("Starting the console thread")
		ConsoleInputThread.start()

		logger.info("Registering the standard types (blocks, items, ...)")
		GameRegistry.autoRegister()

		logger.info("Loading the worlds")
		loadWorlds()

		logger.info("Loading the config")
		Config.load()

		logger.info("Loading the Photon's commands")
		loadCommands()

		logger.info("Loading the plugins")
		loadPlugins()

		logger.info("Starting the TCP server")
		startTcp()

		logger.info(s"Done! You can connect on port ${Config.port}.")
	}

	private def loadDirs(): Unit = {
		Seq(DirMain, DirConfig, DirPlugins, DirWorlds, DirLogs)
		.foreach(_.createIfNotExists(asDirectory = true))
	}

	private def startTcp(): Unit = {
		protocolLibAdapter = new ProtocolLibAdapter(Config.port)
		protocolLibAdapter.start()
	}

	private def loadWorlds(): Unit = {
		for (dir <- DirWorlds.list if dir.isDirectory) {
			logger.debug(s"Registering world ${dir.name}")
			val world = new World(dir.name, WorldType.OVERWORLD)
			registerWorld(world)
			//TODO load world config and spawn region
		}
	}

	private def loadCommands(): Unit = {
		CommandSystem.global.register(new StopCommand)
	}

	/** Loads the plugins that are in DirPlugins. Must be called after loadWorlds(). */
	private def loadPlugins(): Unit = {
		val graph = new DependencyGraph()
		for (file <- DirPlugins.children) {
			logger.debug(s"Analysing ${file.name}")
			val inspection = PluginInfos.inspect(file)
			inspection match {
				case Success(infos) =>
					logger.debug(s"Got infos $infos")
					graph.register(infos)
				case Failure(t) =>
					logger.debug(s"Got failure $t")
					logger.error(s"Error while loading $file", t)
			}
		}
		logger.debug("Building the dependencies graph")
		graph.build()
		logger.debug("Resolving the dependencies")
		val solution = graph.resolve()
		val nbError = solution.errors.size
		if (nbError > 0) {
			logger.warn(s"$nbError out of ${solution.resolvedItems.size} items couldn't be " +
				s"resolved. See below.")
			solution.errors.foreach(s => logger.warn("    " + s))
		}
		for (resolved <- solution.resolvedItems) {
			breakable {
				val pluginClass = resolved.loader.loadClass(resolved.infos.pluginClassName)
				val pluginInstance = pluginClass.newInstance().asInstanceOf[Plugin]
				if (!classOf[GlobalPlugin].isAssignableFrom(pluginClass) && !classOf[WorldPlugin].isAssignableFrom(pluginClass)) {
					logger.warn(s"Plugin $pluginClass is not a GlobalPlugin nor a WorldPlugin.")
					break
				}
				if (classOf[GlobalPlugin].isAssignableFrom(pluginClass)) {
					GlobalPluginSystem.enable(pluginInstance.asInstanceOf[GlobalPlugin])
				}
				// WorldPluginsConfig now handles Global <> World plugin type
				for (enabledPluginWorld <- WorldPluginsConfig.getEnabledPluginWorlds(this, pluginInstance)) {
					enabledPluginWorld.pluginSystem.enable(pluginInstance)
				}
			}
		}
	}

	private[mcphoton] def shutdown(): Unit = {
		// TODO
		logger.warn("Shutdown is not properly implemented yet!")
		logger.debug("Saving the worlds... actually not because it's not ready yet ^_^")
		for (world <- worldsNameMap.valuesIterator) {
			//TODO world.save()
		}
		logger.info("Stopping the TCP server")
		protocolLibAdapter.stop()

		logger.info("Shutdown.")
		System.exit(0)
	}

	Runtime.getRuntime.addShutdownHook(new Thread(() => {
		AnsiConsole.systemUninstall()
		LoggingService.close()
	}))
}
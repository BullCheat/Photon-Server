package org.mcphoton.server

import java.awt.image.BufferedImage
import java.util.UUID
import java.util.concurrent.ConcurrentSkipListSet

import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig
import com.electronwill.nightconfig.core.conversion.{Conversion, ObjectConverter, SpecIntInRange}
import com.electronwill.nightconfig.core.file.CommentedFileConfig
import org.mcphoton.plugin.{GlobalPlugin, Plugin}
import org.mcphoton.server.ServerConfig.{LocationConverter, LogLevelConverter}
import org.mcphoton.world.{Location, World}
import org.slf4j.impl.PhotonLogger

/**
  * @author BullCheat
  */
class WorldPluginsConfig {
  @transient
  private val file = PhotonServer.DirPlugins / "plugins.toml"

  @transient
  @volatile
  private var savedComments: java.util.Map[String, UnmodifiableCommentedConfig.CommentNode] = _


  def load(): Unit = {
    val conf = CommentedFileConfig.builder(file.toJava)
      .defaultResource("/default-plugins-config.toml").build()
    conf.load()
    new ObjectConverter().toObject(conf, this)
    savedComments = conf.getComments
  }

  // Dummy function, returns a list of worlds where the specified plugin should be enabled
  def getEnabledPluginWorlds(server: PhotonServer.type, plugin: Plugin): List[World] =
    if (classOf[GlobalPlugin].isAssignableFrom(plugin.getClass))
      List[World](server.Config.spawnLocation.world) // Fixme le plugin doit-il être initialisé sur un seul monde ou sur tous les mondes ?
    else
      server.enabledWorlds // Fixme lire depuis la config et renvoyer les worlds pour lequel le plugin doit être activé

}

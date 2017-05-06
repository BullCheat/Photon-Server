/*
 * Copyright (c) 2016 MCPhoton <http://mcphoton.org> and contributors.
 *
 * This file is part of the Photon Server Implementation <https://github.com/mcphoton/Photon-Server>.
 *
 * The Photon Server Implementation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Photon Server Implementation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mcphoton;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import org.mcphoton.command.GlobalCommandRegistry;
import org.mcphoton.impl.GameRegistryImpl;
import org.mcphoton.impl.command.GlobalCommandRegistryImpl;
import org.mcphoton.impl.server.Main;
import org.mcphoton.permissions.GlobalPermissionsManager;
import org.mcphoton.server.Server;

/**
 * Implementation of the Photon's class, which is the centralized API core.
 *
 * @author TheElectronWill
 */
public final class Photon {

	public static final File MAIN_DIR = new File(System.getProperty("user.dir")),
			PLUGINS_DIR = new File(MAIN_DIR, "plugins"),
			WORLDS_DIR = new File(MAIN_DIR, "worlds");
	private static final GameRegistry GAME_REGISTRY = new GameRegistryImpl();
	private static final GlobalCommandRegistry COMMAND_REGISTRY = new GlobalCommandRegistryImpl();
	private static final boolean CONSOLE_ADVANCED = !System.getProperty("os.name")
														   .toLowerCase()
														   .contains("windows");

	private Photon() {}

	public static GameRegistry getGameRegistry() {
		return GAME_REGISTRY;
	}

	/**
	 * Gets the Photon's ScheduledExecutorService, which is used to schedule tasks across multiple
	 * threads.
	 * <h2>What kind of task may be submitted to this ExecutorService?</h2>
	 * <p>
	 * To achieve better performance, the submitted tasks:
	 * <ol>
	 * <li>Musn't be IO-bound, in order to avoid delaying the other tasks. Use an asynchronous IO
	 * API instead of the ExecutorService.</li>
	 * <li>Musn't be too short, in order to avoid creating too much overhead. It is advised to group
	 * many small tasks together into one bigger task.</li>
	 * </ol>
	 * </p>
	 */
	public static ScheduledExecutorService getExecutorService() {
		return Main.SERVER.executorService.get();
	}

	public static GlobalPermissionsManager getGlobalPermissionsManager() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	public static GlobalCommandRegistry getGlobalCommandRegistry() {
		return COMMAND_REGISTRY;
	}

	public static boolean isClient() {
		return false;
	}

	public static boolean isServer() {
		return true;
	}

	public static boolean isConsoleAdvanced() {
		return CONSOLE_ADVANCED;
	}

	public static String getVersion() {
		return "dev-alpha";
	}

	public static String getMinecraftVersion() {
		return "1.11";
	}

	public static File getMainDirectory() {
		return MAIN_DIR;
	}

	public static File getPluginsDirectory() {
		return PLUGINS_DIR;
	}

	public static Server getServer() {
		return Main.SERVER;
	}
}
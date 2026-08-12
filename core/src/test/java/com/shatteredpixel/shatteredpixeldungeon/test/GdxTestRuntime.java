package com.shatteredpixel.shatteredpixeldungeon.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Boots a single headless libGDX application context for the whole test JVM.
 *
 * Static game code (Messages, SPDSettings/GameSettings, FileUtils, Reflection, ...) touches
 * Gdx.app / Gdx.files / Gdx.graphics as soon as it is loaded, mostly from static initializers.
 * Something has to make those non-null before any such class runs. No real Game/Scene is ever
 * created here - nothing renders and no window opens.
 *
 * Also points the game's save-file system (FileUtils) at a throwaway temp directory, so tests
 * never read or write the real save directory on the machine running them.
 */
final class GdxTestRuntime {

	private static boolean started = false;

	static synchronized void ensureStarted() {
		if (started) return;

		HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
		//a non-positive value disables gdx-backend-headless's automatic render loop/thread;
		//we only need the static Gdx.* fields populated, nothing should be looping or rendering
		config.updatesPerSecond = -1;
		new HeadlessApplication(new ApplicationAdapter() {}, config);

		//normally set by the platform launcher (e.g. DesktopLauncher) from the jar manifest.
		//DeviceCompat.isDebug() dereferences Game.version unconditionally, so it must be
		//non-null before any game class runs. Deliberately does NOT contain "INDEV", so tests
		//see the same DeviceCompat.isDebug() == false path a released build would.
		Game.version = "test-harness";
		Game.versionCode = Integer.MAX_VALUE;

		try {
			Path saveDir = java.nio.file.Files.createTempDirectory("opd-test-save-");
			FileUtils.setDefaultFileProperties(FileType.Absolute, saveDir.toAbsolutePath() + "/");
		} catch (IOException e) {
			throw new IllegalStateException("could not create temp save directory for tests", e);
		}

		started = true;
	}

	private GdxTestRuntime() {}
}

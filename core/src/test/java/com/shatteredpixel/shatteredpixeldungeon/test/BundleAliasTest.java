package com.shatteredpixel.shatteredpixeldungeon.test;

import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.watabou.utils.Bundle;
import com.watabou.utils.Bundlable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the class-rename savegame countermeasure (audit finding #4): {@link Bundle} stores
 * the fully-qualified class name of every {@link Bundlable} it saves, and a renamed/moved class
 * becomes unfindable on load unless {@link Bundle#addAlias(Class, String)} redirects the old
 * name to the new class.
 *
 * Correction to the original audit finding: {@code addAlias} is not actually unused - there is
 * one real call site, in {@code ShatteredPixelDungeon.registerBundleAliases()} (extracted out of
 * the constructor specifically so it - and the alias it registers - can be tested here without
 * constructing a full {@code Game}/client instance). The gap was that nothing verified it works.
 *
 * A {@code Bundle} is simulated "as if written by an old version" by manually constructing one
 * with an old class name in its internal class-name field, using the same key literal
 * ({@code "__className"}) {@code Bundle} itself uses - that field isn't exposed as a public
 * constant, so if a future refactor of {@code Bundle} renames it, these tests should fail loudly
 * rather than silently stop testing anything.
 */
class BundleAliasTest extends GameTestBase {

	private static final String CLASS_NAME_KEY = "__className";

	@Override
	protected boolean generateLevelInSetup() {
		//this test only needs Bundle + the alias registry, not a hero or level
		return false;
	}

	private static Bundle bundleReferencing(String className) {
		Bundle inner = new Bundle();
		inner.put(CLASS_NAME_KEY, className);
		Bundle outer = new Bundle();
		outer.put(Bundle.DEFAULT_KEY, inner);
		return outer;
	}

	@Test
	void unaliasedUnknownClassNameFailsGracefullyInsteadOfCrashing() {
		//this is the failure mode finding #4 warns about: an old/unknown class name in a
		//savegame does not throw, it just silently drops the object. Documented here as the
		//"before" case the other tests guard against.
		Bundle bundle = bundleReferencing(
				"com.shatteredpixel.shatteredpixeldungeon.test.support.NoSuchClassEverRegistered");

		assertNull(bundle.get(Bundle.DEFAULT_KEY));
	}

	@Test
	void registeredAliasRedirectsBundlableInstantiation() {
		String oldName = "com.shatteredpixel.shatteredpixeldungeon.test.support.RenamedStub";
		Bundle.addAlias(AliasTargetStub.class, oldName);

		Bundle bundle = bundleReferencing(oldName);
		Bundlable restored = bundle.get(Bundle.DEFAULT_KEY);

		assertInstanceOf(AliasTargetStub.class, restored);
		assertTrue(((AliasTargetStub) restored).restoreFromBundleWasCalled);
	}

	@Test
	void registeredAliasRedirectsRawClassReference() {
		String oldName = "com.shatteredpixel.shatteredpixeldungeon.test.support.RenamedClassRefStub";
		Bundle.addAlias(AliasTargetStub.class, oldName);

		Bundle bundle = new Bundle();
		bundle.put("cls", oldName);

		assertInstanceOf(Class.class, bundle.getClass("cls"));
		assertEquals(AliasTargetStub.class, bundle.getClass("cls"));
	}

	@Test
	void registeredAliasRedirectsClassArrayEntries() {
		//SpecialRoom/SecretRoom persist their run-state as raw Class[] (see storeRoomsInBundle),
		//a different Bundle code path than a Bundlable object - it needs its own coverage
		String oldName = "com.shatteredpixel.shatteredpixeldungeon.test.support.RenamedArrayStub";
		Bundle.addAlias(AliasTargetStub.class, oldName);

		//simulates the array having been written under the old name, the same way
		//bundleReferencing() does for the single-Bundlable case
		Bundle bundle = new Bundle();
		bundle.put("classes", new String[]{ oldName });

		Class<?>[] resolved = bundle.getClassArray("classes");

		assertInstanceOf(Class.class, resolved[0]);
		assertEquals(AliasTargetStub.class, resolved[0]);
	}

	@Test
	void productionWornKeyAliasResolvesPre330SkeletonKeyName() {
		//exercises the one alias that actually ships today. If someone deletes the addAlias
		//call in ShatteredPixelDungeon.registerBundleAliases(), or renames/moves WornKey again
		//without updating it, this test fails.
		ShatteredPixelDungeon.registerBundleAliases();

		//the pre-v3.3.0 name - note this is a different class from the CURRENT
		//items.artifacts.SkeletonKey, which is an unrelated artifact that happens to share a
		//simple name. The alias disambiguates via the full old package path.
		Bundle bundle = bundleReferencing("com.shatteredpixel.shatteredpixeldungeon.items.keys.SkeletonKey");

		assertInstanceOf(WornKey.class, bundle.get(Bundle.DEFAULT_KEY));
	}

	public static class AliasTargetStub implements Bundlable {
		public boolean restoreFromBundleWasCalled = false;

		@Override
		public void storeInBundle(Bundle bundle) {}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			restoreFromBundleWasCalled = true;
		}
	}
}

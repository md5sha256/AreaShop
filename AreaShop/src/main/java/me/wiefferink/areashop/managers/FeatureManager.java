package me.wiefferink.areashop.managers;

import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.features.CommandsFeature;
import me.wiefferink.areashop.features.DebugFeature;
import me.wiefferink.areashop.features.FeatureFactory;
import me.wiefferink.areashop.features.FriendsFeature;
import me.wiefferink.areashop.features.RegionFeature;
import me.wiefferink.areashop.features.TeleportFeature;
import me.wiefferink.areashop.features.WorldGuardRegionFlagsFeature;
import me.wiefferink.areashop.features.homeaccess.HomeAccessFeature;
import me.wiefferink.areashop.features.signs.SignsFeature;
import me.wiefferink.areashop.regions.GeneralRegion;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Singleton
public class FeatureManager extends Manager {

	private static final Collection<Class<? extends RegionFeature>> globalFeatureClasses = Arrays.asList(
			CommandsFeature.class,
			WorldGuardRegionFlagsFeature.class,
			DebugFeature.class
	);
	// One instance of each feature, registered for event handling
	private final Set<RegionFeature> globalFeatures = new HashSet<>();
	private final Map<Class<? extends RegionFeature>, Function<GeneralRegion, ? extends RegionFeature>> regionFeatureConstructors = new HashMap<>();

	/**
	 * Constructor.
	 */
	@Inject
	FeatureManager(@Nonnull FeatureFactory featureFactory) {
		// Setup constructors for region specific features
		regionFeatureConstructors.put(SignsFeature.class, wrapInstantiator(SignsFeature.class, featureFactory::createSignsFeature));
		regionFeatureConstructors.put(TeleportFeature.class, wrapInstantiator(TeleportFeature.class, featureFactory::createTeleportFeature));
		regionFeatureConstructors.put(FriendsFeature.class, wrapInstantiator(FriendsFeature.class, featureFactory::createFriendsFeature));
		regionFeatureConstructors.put(HomeAccessFeature.class, wrapInstantiator(HomeAccessFeature.class, featureFactory::createHomeAccessFeature));
	}

	public void initializeFeatures(@Nonnull Injector injector) {
		// Instantiate and register global features (one per type, for event handling)
		for(Class<? extends RegionFeature> clazz : globalFeatureClasses) {
			try {
				RegionFeature feature = injector.getInstance(clazz);
				feature.listen();
				globalFeatures.add(feature);
			} catch(ProvisionException e) {
				AreaShop.error("Failed to instantiate global feature:", clazz, e);
			}
		}
	}

	public <T extends RegionFeature> void registerNonGlobalFeature(@Nonnull Class<T> clazz, @Nonnull Function<GeneralRegion, T> constructor) {
		this.regionFeatureConstructors.put(clazz, constructor);
	}

	private <T extends RegionFeature> Function<GeneralRegion, T> wrapInstantiator(Class<T> clazz, Function<GeneralRegion, T> function
	) {
		return region -> {
			try {
				return function.apply(region);
			} catch (Throwable e) {
				AreaShop.error("Failed to instantiate feature", clazz, "for region", region, e, e.getCause());
				return null;
			}
		};
	}

	@Override
	public void shutdown() {
		for(RegionFeature feature : globalFeatures) {
			feature.shutdown();
		}
	}

	/**
	 * Instanciate a feature for a certain region.
	 * @param region       The region to create a feature for
	 * @param featureClazz The class of the feature to create
	 * @return The feature class
	 */
	public RegionFeature getRegionFeature(GeneralRegion region, Class<? extends RegionFeature> featureClazz) {
		return regionFeatureConstructors.get(featureClazz).apply(region);
	}

}

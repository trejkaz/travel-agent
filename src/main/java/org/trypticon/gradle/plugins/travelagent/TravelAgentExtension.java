package org.trypticon.gradle.plugins.travelagent;

import com.google.common.collect.ImmutableList;
import groovy.json.JsonSlurper;
import org.gradle.api.Transformer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Extension holding per-project configuration for Travel Agent.
 */
public class TravelAgentExtension {

    /**
     * Lazy property for enabled status.
     */
    private final Property<Boolean> enabled;

    /**
     * Lazy list of available trips.
     */
    private final ListProperty<Trip> availableTrips;

    /**
     * Spec to match trips to accept.
     */
    private Spec<? super Trip> filter = Specs.SATISFIES_ALL;


    /**
     * Constructs the travel agent.
     *
     * @param objectFactory the object factory.
     * @param providerFactory the provider factory.
     */
    @Inject
    public TravelAgentExtension(@Nonnull ObjectFactory objectFactory, @Nonnull ProviderFactory providerFactory) {
        enabled = objectFactory.property(Boolean.class);
        enabled.set(true);

        availableTrips = objectFactory.listProperty(Trip.class);
        availableTrips.set(providerFactory.provider(TravelAgentExtension::loadPredefinedTrips));
    }


    /**
     * Loads predefined trips from a resource file.
     *
     * @return the trips.
     */
    private static ImmutableList<Trip> loadPredefinedTrips() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>)
                new JsonSlurper().parse(TravelAgentExtension.class.getResource("trips.json"));

        return data.stream()
                .map(record -> new Trip((String) record.get("language"),
                        (String) record.get("country"),
                        (String) record.get("timeZone")))
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Gets whether the travel agent is enabled.
     *
     * @return {@code true} if enabled, {@code false} if disabled.
     */
    public Property<Boolean> getEnabled() {
        return enabled;
    }

    /**
     * Gets the available trips.
     *
     * @return the available trips.
     */
    public ListProperty<Trip> getAvailableTrips() {
        return availableTrips;
    }

    /**
     * Gets the spec to match trips to accept.
     *
     * @return the spec to match trips to accept.
     */
    public Spec<? super Trip> getFilter() {
        return filter;
    }

    /**
     * Adds a spec for a known failing combination. Sometimes this sort of thing can't be avoided.
     * For instance, Gradle themselves seemingly refuse to fix their own issues with running in Turkish,
     * which makes our own integration tests fail since they are running Gradle.
     *
     * @param spec the spec to match failing trips.
     */
    public void knownFailing(Spec<Trip> spec) {
        filter = Specs.intersect(filter, Specs.negate(spec));
    }

    /**
     * Forces the travel agent to choose the given settings.
     *
     * @param language the language to force.
     * @param country the country to force.
     * @param timeZone the time zone to force.
     */
    public void prefer(Object language, Object country, Object timeZone) {
        if (language != null) {
            filter = Specs.intersect(filter, trip -> trip.getLanguage().equals(language.toString()));
        }
        if (country != null) {
            filter = Specs.intersect(filter, trip -> trip.getCountry().equals(country.toString()));
        }
        if (timeZone != null) {
            filter = Specs.intersect(filter, trip -> trip.getTimeZone().equals(timeZone.toString()));
        }
    }
}

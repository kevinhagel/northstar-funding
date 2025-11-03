package com.northstar.funding.querygeneration.template;

import com.northstar.funding.domain.GeographicScope;
import org.springframework.stereotype.Component;

/**
 * Maps geographic scopes to location terms.
 *
 * <p>Provides two mapping strategies:
 * <ul>
 *   <li>Keywords: Short location names for traditional search engines</li>
 *   <li>Conceptual: Contextual descriptions for AI-optimized search</li>
 * </ul>
 */
@Component
public class GeographicMapper {

    /**
     * Maps geographic scope to keyword string for traditional search engines.
     *
     * @param scope Geographic scope
     * @return Location keyword(s)
     */
    public String toKeywords(GeographicScope scope) {
        return switch (scope) {
            // Country-specific
            case BULGARIA -> "Bulgaria";
            case ROMANIA -> "Romania";
            case GREECE -> "Greece";
            case SERBIA -> "Serbia";
            case NORTH_MACEDONIA -> "North Macedonia";

            // Regional
            case EASTERN_EUROPE -> "Eastern Europe";
            case BALKANS -> "Balkans, Southeastern Europe";
            case SOUTHEASTERN_EUROPE -> "Southeastern Europe, Balkans";
            case CENTRAL_EUROPE -> "Central Europe";

            // EU-related
            case EU_MEMBER_STATES -> "European Union, EU member states";
            case EU_CANDIDATE_COUNTRIES -> "EU candidate countries, accession countries";
            case EU_ENLARGEMENT_REGION -> "EU enlargement region, Western Balkans";

            // Broader scopes
            case EUROPE -> "Europe";
            case INTERNATIONAL -> "international, global";
            case GLOBAL -> "global, worldwide, international";
        };
    }

    /**
     * Maps geographic scope to conceptual description for AI-optimized search.
     *
     * @param scope Geographic scope
     * @return Contextual geographic description
     */
    public String toConceptualDescription(GeographicScope scope) {
        return switch (scope) {
            // Country-specific
            case BULGARIA ->
                "Bulgaria, focusing on post-transition educational development and EU integration priorities";
            case ROMANIA ->
                "Romania, with emphasis on educational modernization and EU structural fund opportunities";
            case GREECE ->
                "Greece, including both EU member state programs and regional Balkan cooperation";
            case SERBIA ->
                "Serbia, as EU candidate country with focus on accession preparation and regional development";
            case NORTH_MACEDONIA ->
                "North Macedonia, as EU candidate country with emphasis on educational reform and development";

            // Regional
            case EASTERN_EUROPE ->
                "Eastern European countries including post-communist transition economies and educational reform priorities";
            case BALKANS ->
                "Balkan region countries with focus on regional cooperation, stability, and educational development";
            case SOUTHEASTERN_EUROPE ->
                "Southeastern European region including both EU members and candidate countries with shared development priorities";
            case CENTRAL_EUROPE ->
                "Central European countries with focus on educational excellence and regional cooperation";

            // EU-related
            case EU_MEMBER_STATES ->
                "European Union member states with access to EU structural funds, Erasmus+, and cohesion programs";
            case EU_CANDIDATE_COUNTRIES ->
                "EU candidate countries eligible for pre-accession funding, IPA programs, and integration support";
            case EU_ENLARGEMENT_REGION ->
                "EU enlargement region including Western Balkans countries preparing for EU membership";

            // Broader scopes
            case EUROPE ->
                "European countries including both EU and non-EU states with pan-European educational initiatives";
            case INTERNATIONAL ->
                "international and cross-border educational programs without geographic restrictions";
            case GLOBAL ->
                "global funding opportunities available worldwide to educational institutions and programs";
        };
    }
}

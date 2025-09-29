package uk.gov.hmcts.reform.pcs.hearings.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomSearchRequest {

    //classes

    public static class PocRequest {

        public NativeEsQuery nativeEsQuery;
        public List<String> supplementaryData = new ArrayList<>();
    }

    public static class NativeEsQuery {

        public Integer from;
        public Integer size;
        public List<String> sort = new ArrayList<>();
        public PocBoolQuery boolQuery; // extracted subset
    }

    public static class PocBoolQuery {

        public List<PocMatch> mustMatches = new ArrayList<>();
    }

    public static class PocMatch {

        public String field;      // e.g., "reference"
        public String operator;   // e.g., "and"
        public String query;      // e.g., "123"
    }

    //methods
    public static String parse(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        PocRequest req = new PocRequest();

        // supplementary_data
        JsonNode supp = root.path("supplementary_data");
        if (supp.isArray()) {
            for (JsonNode n : supp) {
                if (n.isTextual())
                    req.supplementaryData.add(n.asText());
            }
        }

        // native_es_query
        JsonNode nativeQuery = root.path("native_es_query");
        if (!nativeQuery.isMissingNode()) {
            NativeEsQuery nq = new NativeEsQuery();

            // from/size
            nq.from = nativeQuery.path("from").isInt() ? nativeQuery.path("from").asInt() : null;
            nq.size = nativeQuery.path("size").isInt() ? nativeQuery.path("size").asInt() : null;

            // sort (assumed array of strings; your sample has [])
            JsonNode sortNode = nativeQuery.path("sort");
            if (sortNode.isArray()) {
                for (JsonNode s : sortNode) {
                    if (s.isTextual())
                        nq.sort.add(s.asText());
                }
            }

            // query.bool.must[].match
            JsonNode bool = nativeQuery.path("query").path("bool");
            if (bool.isObject()) {
                PocBoolQuery pbq = new PocBoolQuery();

                JsonNode must = bool.path("must");
                if (must.isArray()) {
                    for (JsonNode mustEntry : must) {
                        JsonNode match = mustEntry.path("match");
                        if (match.isObject()) {
                            // match has a single field: the name of the field (e.g., "reference")
                            Optional<String> fieldName = firstFieldName(match);
                            if (fieldName.isPresent()) {
                                String field = fieldName.get();
                                JsonNode fieldObj = match.path(field);
                                if (fieldObj.isObject()) {
                                    PocMatch m = new PocMatch();
                                    m.field = field;
                                    m.operator = textOrNull(fieldObj.path("operator"));
                                    m.query = textOrNull(fieldObj.path("query"));
                                    pbq.mustMatches.add(m);
                                } else if (fieldObj.isTextual()) {
                                    // match: { "reference": "123" } variant
                                    PocMatch m = new PocMatch();
                                    m.field = field;
                                    m.query = fieldObj.asText();
                                    pbq.mustMatches.add(m);
                                }
                            }
                        }
                    }
                }

                nq.boolQuery = pbq;
            }

            req.nativeEsQuery = nq;
        }

        return convertToSql(req);
    }

    private static Optional<String> firstFieldName(JsonNode obj) {
        var it = obj.fieldNames();
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }

    private static String textOrNull(JsonNode n) {
        return n.isTextual() ? n.asText() : null;
    }

    private static String convertToSql(CustomSearchRequest.PocRequest req) {
        String converted = req.nativeEsQuery.boolQuery.mustMatches.getFirst().query;
        return "SELECT * FROM public.pcs_case\n" +
            "ORDER BY id ASC ";
    }
}



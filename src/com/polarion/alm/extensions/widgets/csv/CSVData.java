/*
 * Copyright (C) 2004-2015 Polarion Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.polarion.alm.extensions.widgets.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.polarion.alm.extensions.widgets.common.Utils;
import com.polarion.alm.extensions.widgets.common.Utils.LoadedContent;
import com.polarion.alm.extensions.widgets.common.Utils.RequiredParameterException;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter.Builder;
import com.polarion.alm.shared.api.model.rp.parameter.MultiParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.StrictMap;

@SuppressWarnings("nls")
public class CSVData {

    private final @NotNull String[][] data;
    private final @NotNull Date timestamp;

    private CSVData(String dataLocation, String separator) throws IOException {
        List<String[]> dataList = new ArrayList<>();
        LoadedContent loadedContent = Utils.loadContent(dataLocation);
        timestamp = loadedContent.timestamp;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(loadedContent.content, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] row = line.split(Pattern.quote(separator));
                dataList.add(row);
            }
        }
        data = dataList.toArray(new String[0][]);
    }

    private CSVData(CompositeParameter parameter) throws IOException, RequiredParameterException {
        this(Utils.getRequiredStringParameterValue(parameter, PARAM_DATA_LOCATION), Utils.getRequiredStringParameterValue(parameter, PARAM_FIELD_SEPARATOR));
    }

    public @NotNull String[] getHeader() {
        return (data.length > 0) ? data[0] : new String[0];
    }

    public @NotNull Date getTimestamp() {
        return timestamp;
    }

    public interface Visitor {
        void visit(int rowNum, @NotNull String[] rowData);
    }

    public void visit(@NotNull Visitor visitor) {
        for (int i = 0; i < data.length; i++) {
            visitor.visit(i, data[i]);
        }
    }

    private static final String PARAM_DATA_SOURCE = "dataSource";
    private static final String PARAM_DATA_LOCATION = "dataLocation";
    private static final String PARAM_FIELD_SEPARATOR = "fieldSeparator";
    private static final String PARAM_ADDITIONAL_DATA_SOURCES = "additionalDataSources";
    private static final String PARAM_NAME = "name";

    private static @NotNull CompositeParameter buildParameter(@NotNull ParameterFactory factory, boolean withName) {
        Builder parameter = factory.composite("Data Source");
        if (withName) {
            parameter.add(PARAM_NAME, factory.string("Name").build());
        }
        return parameter
                .add(PARAM_DATA_LOCATION, factory.string("Data Location").build())
                .add(PARAM_FIELD_SEPARATOR, factory.string("Field Separator").value(",").build())
                .build();
    }

    public static void addParameter(@NotNull StrictMap<String, RichPageParameter> parameters, @NotNull ParameterFactory factory) {
        parameters.put(PARAM_DATA_SOURCE, buildParameter(factory, false));
    }

    public static void addAdditionalParameter(@NotNull StrictMap<String, RichPageParameter> parameters, @NotNull ParameterFactory factory) {
        parameters.put(PARAM_ADDITIONAL_DATA_SOURCES, factory.multi("Additional Data Sources", buildParameter(factory, true)).build());
    }

    public static @NotNull CSVData get(@NotNull RichPageWidgetRenderingContext context) throws IOException, RequiredParameterException {
        return new CSVData((CompositeParameter) context.parameter(PARAM_DATA_SOURCE));
    }

    public static @NotNull Map<String, CSVData> getAll(@NotNull RichPageWidgetRenderingContext context) throws IOException, RequiredParameterException {
        Map<String, CSVData> allData = new HashMap<>();
        CSVData defaultData = new CSVData((CompositeParameter) context.parameter(PARAM_DATA_SOURCE));
        allData.put(null, defaultData);
        MultiParameter<CompositeParameter> additionalParameters = context.parameter(PARAM_ADDITIONAL_DATA_SOURCES);
        for (CompositeParameter additionalParameter : additionalParameters.get()) {
            if (Utils.atLeastOneParameterIsSet(additionalParameter, PARAM_NAME, PARAM_DATA_LOCATION)) {
                String name = Utils.getRequiredStringParameterValue(additionalParameter, PARAM_NAME);
                allData.put(name, new CSVData(additionalParameter));
            }
        }
        return allData;
    }

}

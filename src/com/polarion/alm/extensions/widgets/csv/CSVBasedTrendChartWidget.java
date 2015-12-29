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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

import com.polarion.alm.extensions.widgets.csv.CSVBasedTrendChartWidgetRenderer.Aggregation;
import com.polarion.alm.extensions.widgets.csv.CSVBasedTrendChartWidgetRenderer.Scale;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidget;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;

@SuppressWarnings("nls")
public class CSVBasedTrendChartWidget extends RichPageWidget {

    private static final String ICON_NAME = "trend.png";

    static final String PARAM_DATA_KEY = "dataKey";
    static final String PARAM_COLOR = "color";
    static final String PARAM_NAME = "name";
    static final String PARAM_TO = "to";
    static final String PARAM_FROM = "from";
    static final String PARAM_DATES = "dates";
    static final String PARAM_SERIES = "series";
    static final String PARAM_TITLE = "title";
    static final String PARAM_SCALE = "scale";
    static final String PARAM_AGGREGATION = "aggregation";
    static final String PARAM_YEAR = "year";
    static final String PARAM_TYPE = "type";
    static final String PARAM_TEXT_ABOVE = "textAbove";
    static final String PARAM_TEXT_BELOW = "textBelow";
    static final String PARAM_DATE_FORMAT = "dateFormat";

    @Override
    @NotNull
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return widgetContext.resourceUrl(ICON_NAME);
    }

    @Override
    @NotNull
    public String getLabel(@NotNull SharedContext context) {
        return "CSV-based Trend Chart";
    }

    @Override
    @NotNull
    public String getDetailsHtml(@NotNull RichPageWidgetContext widgetContext) {
        return "Trend chart visualizing data from CSV file stored in repository.";
    }

    @Override
    @NotNull
    public Iterable<String> getTags(@NotNull SharedContext context) {
        return new ImmutableStrictList("CSV", context.localization().getString("richpages.widget.tag.charts"));
    }

    @Override
    @NotNull
    public StrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory factory) {
        StrictMap<String, RichPageParameter> parameters = new StrictMapImpl<String, RichPageParameter>();
        parameters.put(PARAM_TITLE, factory.string("Title").value("Chart Title").build());
        CSVData.addParameter(parameters, factory);
        CSVData.addAdditionalParameter(parameters, factory);
        CompositeParameter serie = factory.composite("Series")
                .add(PARAM_NAME, factory.string("Name").build())
                .add(PARAM_COLOR, factory.string("Color").build())
                .add(PARAM_DATA_KEY, factory.string("Data Key").build())
                .add(PARAM_AGGREGATION, factory.string("Aggregation " + Arrays.asList(Aggregation.values())).value(Aggregation.sum.toString()).build())
                .add(PARAM_TYPE, factory.string("Type [bar, column, line, spline or leave empty for widget-wide setting]").build())
                .build();
        parameters.put(PARAM_SERIES, factory.multi("Data Visualization", serie).build());
        CompositeParameter dates = factory.composite("Dates")
                .add(PARAM_FROM, factory.date("From").build())
                .add(PARAM_TO, factory.date("To").build())
                .add(PARAM_SCALE, factory.string("Scale " + Arrays.asList(Scale.values())).value(Scale.month.toString()).build())
                .add(PARAM_YEAR, factory.string("Year (used instead of From and To)").build())
                .build();
        parameters.put(PARAM_DATES, dates);
        parameters.put(PARAM_TYPE, factory.string("Type [bar, column, line, spline]").value("line").build());
        parameters.put(PARAM_TEXT_ABOVE, factory.string("Text Above").build());
        parameters.put(PARAM_TEXT_BELOW, factory.string("Text Below").build());
        parameters.put(PARAM_DATE_FORMAT, factory.string("Date Format").value("yyyy-MM-dd").build());
        return parameters;
    }

    @Override
    @NotNull
    public String renderHtml(@NotNull RichPageWidgetRenderingContext context) {
        return new CSVBasedTrendChartWidgetRenderer(context).render();
    }

    @Override
    @NotNull
    public InputStream getResourceStream(@NotNull String path) throws IOException {
        if (path.equals(ICON_NAME)) {
            return getClass().getResourceAsStream(path);
        }
        return super.getResourceStream(path);
    }

}

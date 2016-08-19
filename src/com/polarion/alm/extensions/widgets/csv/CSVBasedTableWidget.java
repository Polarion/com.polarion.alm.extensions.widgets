/*
 * Copyright 2015 Polarion AG
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

import org.jetbrains.annotations.NotNull;

import com.polarion.alm.extensions.widgets.common.Utils;
import com.polarion.alm.extensions.widgets.common.Utils.RequiredParameterException;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.model.rp.parameter.ParameterFactory;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidget;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetContext;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.collections.ImmutableStrictList;
import com.polarion.alm.shared.api.utils.collections.StrictMap;
import com.polarion.alm.shared.api.utils.collections.StrictMapImpl;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;

@SuppressWarnings("nls")
public class CSVBasedTableWidget extends RichPageWidget {

    private static final String ICON_NAME = "table.png";

    @Override
    @NotNull
    public String getIcon(@NotNull RichPageWidgetContext widgetContext) {
        return widgetContext.resourceUrl(ICON_NAME);
    }

    @Override
    @NotNull
    public String getLabel(@NotNull SharedContext context) {
        return "CSV-based Table";
    }

    @Override
    @NotNull
    public String getDetailsHtml(@NotNull RichPageWidgetContext widgetContext) {
        return "Table visualizing data from CSV file stored in repository.";
    }

    @Override
    @NotNull
    public Iterable<String> getTags(@NotNull SharedContext context) {
        return new ImmutableStrictList("CSV", context.localization().getString("richpages.widget.tag.generic"));
    }

    @Override
    @NotNull
    public StrictMap<String, RichPageParameter> getParametersDefinition(@NotNull ParameterFactory factory) {
        StrictMap<String, RichPageParameter> parameters = new StrictMapImpl<String, RichPageParameter>();
        CSVData.addParameter(parameters, factory);
        return parameters;
    }

    @Override
    @NotNull
    public String renderHtml(@NotNull RichPageWidgetRenderingContext context) {
        CSVData data;
        try {
            data = CSVData.get(context);
        } catch (RequiredParameterException e) {
            return context.renderWarning(e.getLocalizedMessage());
        } catch (IOException e) {
            return context.renderError(e.getLocalizedMessage());
        }

        HtmlFragmentBuilder builder = context.createHtmlFragmentBuilder();
        final HtmlTagBuilder table = Utils.addTableTag(builder);
        data.visit(new CSVData.Visitor() {

            @Override
            public void visit(int rowNum, @NotNull String[] rowData) {
                boolean header = rowNum == 0;
                appendRow(table, header, rowData);
            }
        });
        return builder.toString();
    }

    private void appendRow(@NotNull HtmlTagBuilder table, boolean header, @NotNull String[] row) {
        HtmlTagBuilder tr = header ? Utils.addHeaderTRTag(table.append()) : Utils.addTRTag(table.append());
        for (String cell : row) {
            HtmlTagBuilder td = header ? tr.append().tag().th() : tr.append().tag().td();
            td.append().text(cell);
        }
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

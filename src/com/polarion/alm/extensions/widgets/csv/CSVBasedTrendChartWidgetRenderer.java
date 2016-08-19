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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.polarion.alm.extensions.widgets.common.Utils;
import com.polarion.alm.extensions.widgets.common.Utils.Dates;
import com.polarion.alm.extensions.widgets.common.Utils.RequiredParameterException;
import com.polarion.alm.extensions.widgets.csv.CSVData.Visitor;
import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.DateParameter;
import com.polarion.alm.shared.api.model.rp.parameter.MultiParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.charts.PolarionChartBuilder;
import com.polarion.alm.shared.api.utils.charts.highcharts.HcSerie;
import com.polarion.alm.shared.api.utils.charts.highcharts.HcSerieData;
import com.polarion.alm.shared.api.utils.charts.highcharts.HcSerieDataObject;
import com.polarion.alm.shared.api.utils.charts.highcharts.HighchartBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.core.util.types.ThreadSafeDateFormatWrapper;

@SuppressWarnings("nls")
public class CSVBasedTrendChartWidgetRenderer {

    private final @NotNull RichPageWidgetRenderingContext context;
    private/*final*/@Nullable String title;
    private/*final @NotNull*/
    MultiParameter<CompositeParameter> series;
    private/*final @NotNull*/
    Scale scale;
    private/*final @NotNull*/
    String type;
    private/*final*/@Nullable String textAbove;
    private/*final*/@Nullable String textBelow;
    private/*final @NotNull*/ DateFormat dateFormat;
    private/*final @NotNull*/
    Map<Date, Map<String, Integer>> data;
    private final @NotNull Map<String, Date> timestamps = new HashMap<>();
    private/*@Nullable*/String warning;
    private/*@Nullable*/String error;

    public CSVBasedTrendChartWidgetRenderer(@NotNull RichPageWidgetRenderingContext context) {
        this.context = context;
        try {
            title = Utils.getStringParameterValue(context, CSVBasedTrendChartWidget.PARAM_TITLE);
            series = context.parameter(CSVBasedTrendChartWidget.PARAM_SERIES);
            CompositeParameter datesParam = context.parameter(CSVBasedTrendChartWidget.PARAM_DATES);
            scale = Scale.valueOf(Utils.getRequiredStringParameterValue(datesParam, CSVBasedTrendChartWidget.PARAM_SCALE));
            String year = Utils.getStringParameterValue(datesParam, CSVBasedTrendChartWidget.PARAM_YEAR);
            type = Utils.getRequiredStringParameterValue(context, CSVBasedTrendChartWidget.PARAM_TYPE);
            String textAbove = Utils.getStringParameterValue(context, CSVBasedTrendChartWidget.PARAM_TEXT_ABOVE);
            String textBelow = Utils.getStringParameterValue(context, CSVBasedTrendChartWidget.PARAM_TEXT_BELOW);
            dateFormat = new ThreadSafeDateFormatWrapper(new SimpleDateFormat(Utils.getRequiredStringParameterValue(context, CSVBasedTrendChartWidget.PARAM_DATE_FORMAT)));

            Dates dates = initDates(datesParam, year);

            Map<String, CSVData> csvData = CSVData.getAll(context);
            data = loadData(csvData, dates);
            for (Map.Entry<String, CSVData> csvDataEntry : csvData.entrySet()) {
                timestamps.put(csvDataEntry.getKey(), csvDataEntry.getValue().getTimestamp());
            }

            String[] texts = processTexts(textAbove, textBelow, dates);
            this.textAbove = texts[0];
            this.textBelow = texts[1];
        } catch (RequiredParameterException e) {
            warning = e.getLocalizedMessage();
        } catch (Exception e) {
            error = e.getLocalizedMessage();
        }
    }

    private @NotNull Dates initDates(@NotNull CompositeParameter datesParam, @Nullable String year) {
        if (year != null) {
            return new Dates(Utils.date(Integer.parseInt(year), 0, 1), Utils.date(Integer.parseInt(year), 11, 31));
        }
        return new Dates(((DateParameter) datesParam.get(CSVBasedTrendChartWidget.PARAM_FROM)).value(),
                ((DateParameter) datesParam.get(CSVBasedTrendChartWidget.PARAM_TO)).value());
    }

    private @NotNull String[] processTexts(@Nullable String textAbove, @Nullable String textBelow, @NotNull Dates dates) {
        int workingDays = dates.calculateWorkingDaysBetween();

        Map<String, Integer> statistics = computeStatistics(data);
        if (textAbove != null) {
            textAbove = processText(textAbove, statistics, workingDays);
        }
        if (textBelow != null) {
            textBelow = processText(textBelow, statistics, workingDays);
        }
        return new String[] { textAbove, textBelow };
    }

    @NotNull
    public String render() {
        if (error != null) {
            return context.renderError(error);
        }
        if (warning != null) {
            return context.renderWarning(warning);
        }
        try {
            HtmlFragmentBuilder builder = context.createHtmlFragmentBuilder();
            if (textAbove != null) {
                builder.tag().p().append().text(textAbove);
            }
            renderChart(builder);
            if (textBelow != null) {
                builder.tag().p().append().text(textBelow);
            }
            return builder.toString();
        } catch (RequiredParameterException e) {
            return context.renderWarning(e.getLocalizedMessage());
        }
    }

    private void renderChart(@NotNull HtmlFragmentBuilder builder) throws RequiredParameterException {
        PolarionChartBuilder chartBuilder = context.createChartBuilder();
        chartBuilder.title(title);

        HighchartBuilder hcBuilder = chartBuilder.build();
        hcBuilder.chart().addRawAttribute("type", "'" + type + "'");
        hcBuilder.chart().zoomType().x(true);
        hcBuilder.xAxis().type().datetime().addRawAttribute("labels", "{ rotation: -45 }");
        hcBuilder.yAxis().title().text(null);
        hcBuilder.plotOptions().series().addRawAttribute("marker", "{ enabled: false }");

        int minValue = 0;

        for (CompositeParameter serie : series.get()) {
            if (Utils.atLeastOneParameterIsSet(serie, CSVBasedTrendChartWidget.PARAM_NAME, CSVBasedTrendChartWidget.PARAM_COLOR, CSVBasedTrendChartWidget.PARAM_DATA_KEY)) {
                int minSerieValue = buildSerie(hcBuilder, serie);
                minValue = Math.min(minValue, minSerieValue);
            }
        }

        hcBuilder.yAxis().addRawAttribute("min", minValue + "");

        hcBuilder.render(builder, context.columnWidth() / 3, context.columnWidth());

    }

    private int buildSerie(@NotNull HighchartBuilder hcBuilder, @NotNull CompositeParameter serie) throws RequiredParameterException {
        String name = Utils.getStringParameterValue(serie, CSVBasedTrendChartWidget.PARAM_NAME);
        String color = Utils.getStringParameterValue(serie, CSVBasedTrendChartWidget.PARAM_COLOR);
        String dataKey = Utils.getRequiredStringParameterValue(serie, CSVBasedTrendChartWidget.PARAM_DATA_KEY);
        Aggregation aggregation = Aggregation.valueOf(Utils.getRequiredStringParameterValue(serie, CSVBasedTrendChartWidget.PARAM_AGGREGATION));
        String type = Utils.getStringParameterValue(serie, CSVBasedTrendChartWidget.PARAM_TYPE);

        Map<Date, Integer> serieRawData = filterDataByKey(dataKey);

        HcSerie hcSerie = hcBuilder.series().add();
        if (type != null) {
            hcSerie.addRawAttribute("type", "'" + type + "'");
        }
        if (name != null) {
            hcSerie.name(name);
        }
        HcSerieData hcSerieData = hcSerie.color(color).data();

        return plotScaledData(serieRawData, hcSerieData, aggregation);
    }

    private int plotScaledData(@NotNull Map<Date, Integer> serieRawData, @NotNull HcSerieData hcSerieData, @NotNull Aggregation aggregation) {
        int minValue = Integer.MAX_VALUE;

        Map<Date, Integer> serieScaledRawData = scaleSerieRawData(serieRawData, scale, aggregation);
        for (Map.Entry<Date, Integer> serieDataEntry : serieScaledRawData.entrySet()) {
            HcSerieDataObject serieDataPoint = hcSerieData.add();
            serieDataPoint.x(serieDataEntry.getKey());
            int value = serieDataEntry.getValue();
            minValue = Math.min(minValue, value);
            serieDataPoint.y(value);
        }

        return minValue;
    }

    private @NotNull Map<Date, Integer> filterDataByKey(@NotNull String dataKey) {
        Map<Date, Integer> serieRawData = new LinkedHashMap<>();
        for (Map.Entry<Date, Map<String, Integer>> dataEntry : data.entrySet()) {
            Date date = dataEntry.getKey();

            Map<String, Integer> items = dataEntry.getValue();
            Integer number = items.get(dataKey);
            if (number != null) {
                serieRawData.put(date, number);
            }
        }
        return serieRawData;
    }

    private @NotNull String processText(@NotNull String text, @NotNull Map<String, Integer> statistics, int workingDays) {
        text = text.replace("${_workingDays}", workingDays + "");
        for (Map.Entry<String, Integer> statisticsEntry : statistics.entrySet()) {
            String key = statisticsEntry.getKey().replaceAll("\\s+", "");
            int value = statisticsEntry.getValue();
            text = text.replace("${" + key + "}", value + "");
            float perDay = (workingDays > 0) ? value / (float) workingDays : 0;
            text = text.replace("${" + key + "PerDay}", String.format("%.1f", perDay));
        }
        for (Map.Entry<String, Date> timestampsEntry : timestamps.entrySet()) {
            String key = timestampsEntry.getKey();
            if (key == null) {
                key = "";
            }
            Date timestamp = timestampsEntry.getValue();
            text = text.replace("${_timestamp" + key + "}", dateFormat.format(timestamp));
        }
        text = text.replaceAll("\\$\\{.*?\\}", "0");
        return text;
    }

    private @NotNull Map<String, Integer> computeStatistics(@NotNull Map<Date, Map<String, Integer>> data) {
        Map<String, Integer> statistics = new LinkedHashMap<>();
        for (Map<String, Integer> dataItems : data.values()) {
            for (Map.Entry<String, Integer> dataItem : dataItems.entrySet()) {
                Integer number = statistics.get(dataItem.getKey());
                if (number == null) {
                    number = 0;
                }
                statistics.put(dataItem.getKey(), number + dataItem.getValue());
            }
        }
        return statistics;
    }

    private @NotNull Map<Date, Integer> scaleSerieRawData(@NotNull Map<Date, Integer> serieRawData, @NotNull Scale scale, @NotNull Aggregation aggregation) {
        Map<Date, Integer> scaled = new TreeMap<>();
        for (Map.Entry<Date, Integer> dataEntry : serieRawData.entrySet()) {
            Date scaledDate = scale.scaleDate(dataEntry.getKey());
            Integer stored = scaled.get(scaledDate);
            int computed = aggregation.compute(stored, dataEntry.getValue());
            scaled.put(scaledDate, computed);
        }
        return scaled;
    }

    private static final DateFormat xmlDateFormat = new ThreadSafeDateFormatWrapper(new SimpleDateFormat("yyyy-MM-dd"));

    enum Scale {
        day {
            @Override
            protected void scaleDate(@NotNull Calendar cal) {
                // no change
            }
        },
        week {
            @Override
            protected void scaleDate(@NotNull Calendar cal) {
                cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            }
        },
        month {
            @Override
            protected void scaleDate(@NotNull Calendar cal) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
            }
        },
        year {
            @Override
            protected void scaleDate(@NotNull Calendar cal) {
                cal.set(Calendar.DAY_OF_YEAR, 1);
            }
        };

        public @NotNull Date scaleDate(@NotNull Date date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            scaleDate(cal);
            return cal.getTime();
        }

        protected abstract void scaleDate(@NotNull Calendar cal);
    }

    enum Aggregation {
        sum {
            @Override
            public int compute(@Nullable Integer stored, int current) {
                return (stored == null) ? current : stored + current;
            }
        },
        first {
            @Override
            public int compute(@Nullable Integer stored, int current) {
                return (stored == null) ? current : stored;
            }
        },
        last {
            @Override
            public int compute(@Nullable Integer stored, int current) {
                return current;
            }
        },
        min {
            @Override
            public int compute(@Nullable Integer stored, int current) {
                return (stored == null) ? current : Math.min(stored, current);
            }
        },
        max {
            @Override
            public int compute(@Nullable Integer stored, int current) {
                return (stored == null) ? current : Math.max(stored, current);
            }
        };

        abstract public int compute(@Nullable Integer stored, int current);
    }

    private @NotNull Map<Date, Map<String, Integer>> loadData(@NotNull Map<String, CSVData> csvData, @NotNull final Dates dates) {
        Map<Date, Map<String, Integer>> data = new TreeMap<>();
        for (Map.Entry<String, CSVData> csvDataEntry : csvData.entrySet()) {
            String keyPrefix = csvDataEntry.getKey() == null ? "" : csvDataEntry.getKey() + ".";
            loadData(data, keyPrefix, csvDataEntry.getValue(), dates);
        }
        return data;
    }

    private void loadData(final @NotNull Map<Date, Map<String, Integer>> data, @NotNull final String keyPrefix, @NotNull CSVData csvData, @NotNull final Dates dates) {
        final String[] header = csvData.getHeader();
        csvData.visit(new Visitor() {
            @Override
            public void visit(int rowNum, @NotNull String[] rowData) {
                if (rowNum > 0) {
                    if (rowData.length > 0) {
                        Date date;
                        try {
                            date = xmlDateFormat.parse(rowData[0]);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                        if (date.getTime() >= dates.from.getTime() && date.getTime() <= dates.to.getTime()) {
                            Map<String, Integer> dataForDate = data.get(date);
                            if (dataForDate == null) {
                                dataForDate = new HashMap<>();
                            }
                            for (int i = 1; i < rowData.length; i++) {
                                String key = keyPrefix + header[i];
                                int value = Integer.parseInt(rowData[i]);
                                dataForDate.put(key, value);
                            }
                            data.put(date, dataForDate);
                        }
                    }
                }
            }
        });
    }
}

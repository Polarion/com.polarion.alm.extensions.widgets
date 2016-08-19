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
package com.polarion.alm.extensions.widgets.common;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.polarion.alm.shared.api.model.rp.parameter.CompositeParameter;
import com.polarion.alm.shared.api.model.rp.parameter.RichPageParameter;
import com.polarion.alm.shared.api.model.rp.parameter.StringParameter;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetRenderingContext;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.calendar.IWorkingCalendar;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;

@SuppressWarnings("nls")
public class Utils {

    public static @NotNull Date date(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        return cal.getTime();
    }

    public static @Nullable String getStringParameterValue(@NotNull StringParameter parameter) {
        String value = parameter.value();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    public static @NotNull String getRequiredStringParameterValue(@NotNull StringParameter parameter) throws RequiredParameterException {
        String value = getStringParameterValue(parameter);
        if (value == null) {
            throw new RequiredParameterException(parameter);
        }
        return value;
    }

    public static @Nullable String getStringParameterValue(@NotNull RichPageWidgetRenderingContext context, @NotNull String id) {
        return getStringParameterValue((StringParameter) context.parameter(id));
    }

    public static @NotNull String getRequiredStringParameterValue(@NotNull RichPageWidgetRenderingContext context, @NotNull String id) throws RequiredParameterException {
        StringParameter parameter = context.parameter(id);
        return getRequiredStringParameterValue(parameter);
    }

    public static @Nullable String getStringParameterValue(@NotNull CompositeParameter parent, @NotNull String id) {
        return getStringParameterValue((StringParameter) parent.get(id));
    }

    public static @NotNull String getRequiredStringParameterValue(@NotNull CompositeParameter parent, @NotNull String id) throws RequiredParameterException {
        return getRequiredStringParameterValue((StringParameter) parent.get(id));
    }

    public static boolean atLeastOneParameterIsSet(@NotNull CompositeParameter parent, @NotNull String... ids) {
        for (String id : ids) {
            String value = getStringParameterValue(parent, id);
            if (value != null) {
                return true;
            }
        }
        return false;
    }

    public static final class RequiredParameterException extends Exception {
        private static final long serialVersionUID = 1L;

        public RequiredParameterException(RichPageParameter parameter) {
            super("Parameter '" + parameter.label() + "' is required");
        }

    }

    static IWorkingCalendar getWorkingCalendar() {
        return PlatformContext.getPlatform().lookupService(ITrackerService.class).getPlanningManager().getDefaultWorkingCalendar();
    }

    public static final class Dates {
        public final @NotNull Date from;
        public final @NotNull Date to;

        public Dates(@NotNull Date from, @NotNull Date to) {
            this.from = from;
            this.to = to;
        }

        public int calculateWorkingDaysBetween() {
            return getWorkingCalendar().numberOfWorkingDays(from, to);
        }

        @Override
        public String toString() {
            return "Dates [from=" + from + ", to=" + to + "]";
        }
    }

    public static final class LoadedContent {
        public LoadedContent(@NotNull InputStream content, @NotNull Date timestamp) {
            super();
            this.content = content;
            this.timestamp = timestamp;
        }

        public final @NotNull InputStream content;
        public final @NotNull Date timestamp;
    }

    public static @NotNull LoadedContent loadContent(@NotNull String dataLocation) {
        String baseline = getCurrentBaseline();
        ILocation loc = Location.getLocation(IRepositoryService.DEFAULT, dataLocation, baseline);
        IRepositoryService repositoryService = PlatformContext.getPlatform().lookupService(IRepositoryService.class);
        IRepositoryReadOnlyConnection connection = repositoryService.getReadOnlyConnection(loc);
        InputStream contentStream = connection.getContent(loc);
        Date lastChangedDate = connection.getResourceProperties(loc).getLastChangedDate();
        return new LoadedContent(contentStream, lastChangedDate);
    }

    private static @Nullable String getCurrentBaseline() {
        IDataService dataService = PlatformContext.getPlatform().lookupService(IDataService.class);
        return dataService.getCurrentBaselineRevision();
    }

    public static @NotNull HtmlTagBuilder addTableTag(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder tag = builder.tag().table();
        tag.attributes().className("polarion-rpw-table-content");
        return tag;
    }

    public static @NotNull HtmlTagBuilder addHeaderTRTag(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder tag = builder.tag().tr();
        tag.attributes().className("polarion-rpw-table-header-row");
        return tag;
    }

    public static @NotNull HtmlTagBuilder addTRTag(@NotNull HtmlContentBuilder builder) {
        HtmlTagBuilder tag = builder.tag().tr();
        tag.attributes().className("polarion-rpw-table-content-row");
        return tag;
    }

}

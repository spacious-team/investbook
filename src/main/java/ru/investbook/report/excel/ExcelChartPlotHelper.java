/*
 * InvestBook
 * Copyright (C) 2020  Vitalii Ananev <spacious-team@ya.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.investbook.report.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickLabelPosition;
import org.apache.poi.xddf.usermodel.chart.AxisTickMark;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.DisplayBlanks;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.util.function.BiConsumer;

@Slf4j
public class ExcelChartPlotHelper {

    static void plotChart(String name, Sheet sheet, BiConsumer<String, XSSFSheet> plotter) {
        try {
            plotter.accept(name, (XSSFSheet) sheet);
        } catch (Exception e) {
            String message = "Не могу построить график '{}' на вкладке '{}'";
            log.info(message, name, sheet.getSheetName());
            log.debug(message, name, sheet.getSheetName(), e);
        }
    }

    static XSSFChart createChart(XSSFSheet sheet, String name, int positionX, int positionY, int width, int height) {
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0,
                positionX, positionY, positionX + width, positionY + height);

        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(name);
        chart.setTitleOverlay(false);
        return chart;
    }

    static XDDFChartData createScatterChartData(XSSFChart chart) {
        setScatterChartStyle(chart);
        return chart.createData(ChartTypes.SCATTER,
                chart.getAxes().get(0),
                (XDDFValueAxis) chart.getAxes().get(1));
    }

    static XDDFChartData createPieChartData(XSSFChart chart) {
        setPieChartStyle(chart);
        XDDFChartData data = chart.createData(ChartTypes.PIE, null, null);
        data.setVaryColors(true);
        return data;
    }

    private static void setScatterChartStyle(XSSFChart chart) {
        chart.displayBlanksAs(DisplayBlanks.SPAN); // соединять линиями соседние точки, при наличии пустых ячеек
        setChartBorderStyle(chart);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);
        legend.setOverlay(false);
        XDDFValueAxis bottomAxis = chart.createValueAxis(AxisPosition.BOTTOM);
        XDDFLineProperties lineProperties = getLineProperties();
        setAxisStyle(bottomAxis, lineProperties);
        bottomAxis.setTickLabelPosition(AxisTickLabelPosition.LOW);
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        setAxisStyle(leftAxis, lineProperties);
    }

    private static void setPieChartStyle(XSSFChart chart) {
        setChartBorderStyle(chart);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);
    }

    private static void setChartBorderStyle(XSSFChart chart) {
        chart.getCTChartSpace()
                .addNewRoundedCorners()
                .setVal(false);
        chart.getCTChartSpace()
                .addNewSpPr()
                .addNewLn()
                .addNewSolidFill()
                .addNewSrgbClr()
                .setVal(new byte[]{(byte) 200, (byte) 200, (byte) 200});
    }

    private static XDDFLineProperties getLineProperties() {
        XDDFSolidFillProperties fillProperties = new  XDDFSolidFillProperties();
        fillProperties.setColor(XDDFColor.from(PresetColor.LIGHT_GRAY));
        XDDFLineProperties lineProperties = new XDDFLineProperties();
        lineProperties.setFillProperties(fillProperties);
        return lineProperties;
    }

    private static void setAxisStyle(XDDFValueAxis axis, XDDFLineProperties lineProperties) {
        axis.setCrosses(AxisCrosses.AUTO_ZERO);
        axis.setMajorTickMark(AxisTickMark.NONE);
        axis.setMinorTickMark(AxisTickMark.NONE);
        axis.getOrAddShapeProperties().setLineProperties(lineProperties);
        axis.getOrAddMajorGridProperties().setLineProperties(lineProperties);
    }


    static void disableScatterVaryColors(XSSFChart chart) {
        chart.getCTChart()
                .getPlotArea()
                .getScatterChartArray(0)
                .addNewVaryColors()
                .setVal(false);
    }

    static CellRangeAddress nonEmptyCellRangeAddress(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        int count = 0;
        for (int i = firstRow; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            for (int j = firstCol; j <= lastCol; j++) {
                if (row.getCell(j) != null) {
                    count++;
                }
                if (count > 1) { // for plotting graph 2 or more values requires
                    return new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
                }
            }
        }
        throw new IllegalArgumentException("No value in cell range");
    }
}

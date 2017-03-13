/*
 * Copyright 2006-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.excel.poi;

import java.io.Closeable;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.item.excel.AbstractExcelItemReader;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.core.io.Resource;

/**
 * {@link org.springframework.batch.item.ItemReader} implementation which uses apache POI to read an Excel
 * file. It will read the file sheet for sheet and row for row. It is based on
 * the {@link org.springframework.batch.item.file.FlatFileItemReader}
 *
 * @param <R> Type used for representing a single row, such as an array
 * @param <T> the type
 * @author Marten Deinum
 * @since 0.5.0
 */
public class PoiItemReader<R,T> extends AbstractExcelItemReader<R,T> {

    private Workbook workbook;

    private InputStream workbookStream;
    
    private final PoiSheetFactory<R> poiSheetFactory;

    public PoiItemReader(PoiSheetFactory<R> poiSheetFactory) {
        this.poiSheetFactory = poiSheetFactory;
    }
    
    /** Create new instance that represents each row as a string array */
    public static <T> PoiItemReader<String[], T> newStringArrayItemInstance() {
        return new PoiItemReader<String[], T>(new StringArrayPoiSheetFactory());
    }

    @Override
    protected Sheet<R> getSheet(final int sheet) {
        return poiSheetFactory.newPoiSheet(this.workbook.getSheetAt(sheet));
    }

    @Override
    protected int getNumberOfSheets() {
        return this.workbook.getNumberOfSheets();
    }

    @Override
    protected void doClose() throws Exception {
        // As of Apache POI 3.11 there is a close method on the Workbook, prior version
        // lack this method.
        if (workbook instanceof Closeable) {
            this.workbook.close();
        }

        if (workbookStream != null) {
            workbookStream.close();
        }
        this.workbook=null;
        this.workbookStream=null;
    }

    /**
     * Open the underlying file using the {@code WorkbookFactory}. We keep track of the used {@code InputStream} so that
     * it can be closed cleanly on the end of reading the file. This to be able to release the resources used by
     * Apache POI.
     *
     * @param resource the {@code Resource} pointing to the Excel file.
     * @throws Exception is thrown for any errors.
     */
    @Override
    protected void openExcelFile(final Resource resource) throws Exception {
        workbookStream = resource.getInputStream();
        if (!workbookStream.markSupported() && !(workbookStream instanceof PushbackInputStream)) {
            throw new IllegalStateException("InputStream MUST either support mark/reset, or be wrapped as a PushbackInputStream");
        }
        this.workbook = WorkbookFactory.create(workbookStream);
        this.workbook.setMissingCellPolicy(Row.CREATE_NULL_AS_BLANK);
    }

}

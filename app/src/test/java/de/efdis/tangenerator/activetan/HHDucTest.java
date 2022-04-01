/*
 * Copyright (c) 2019 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
 *
 * This file is part of the activeTAN app for Android.
 *
 * The activeTAN app is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The activeTAN app is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the activeTAN app.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.efdis.tangenerator.activetan;

import junit.framework.TestCase;

import org.junit.Test;

import java.math.BigDecimal;

public class HHDucTest {

    @Test
    public void twoDataElementsExample() throws HHDuc.UnsupportedDataFormatException {
        byte[] data;
        {
            HHDuc hhduc = new HHDuc(VisualisationClass.CREDIT_TRANSFER_SEPA);

            hhduc.setDataElement(DataElementType.IBAN_RECIPIENT, "DE1234");
            hhduc.setDataElement(DataElementType.AMOUNT, new BigDecimal("47.11"));

            data = hhduc.getBytes();
        }

        HHDuc hhDuc = HHDuc.parse(data);

        TestCase.assertEquals(VisualisationClass.CREDIT_TRANSFER_SEPA, hhDuc.getVisualisationClass());
        TestCase.assertEquals("DE1234", hhDuc.getDataElement(DataElementType.IBAN_RECIPIENT));
        TestCase.assertEquals("47,11", hhDuc.getDataElement(DataElementType.AMOUNT));
    }

}

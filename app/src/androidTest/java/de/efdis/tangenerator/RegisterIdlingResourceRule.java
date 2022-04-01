/*
 * Copyright (c) 2022 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

package de.efdis.tangenerator;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

import org.junit.rules.ExternalResource;

public class RegisterIdlingResourceRule extends ExternalResource {
    private final IdlingResource idlingResource;

    public RegisterIdlingResourceRule(IdlingResource idlingResource) {
        this.idlingResource = idlingResource;
    }

    @Override
    protected void before() throws Throwable {
        IdlingRegistry.getInstance().register(idlingResource);
    }

    @Override
    protected void after() {
        IdlingRegistry.getInstance().unregister(idlingResource);
    }
}

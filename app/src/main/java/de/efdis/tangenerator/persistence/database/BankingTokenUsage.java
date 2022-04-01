/*
 * Copyright (c) 2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

package de.efdis.tangenerator.persistence.database;

public enum BankingTokenUsage {
    /**
     * No user authentication is required for using the banking token.
     * However, the device must have been unlocked.
     */
    DISABLED_AUTH_PROMPT,

    /**
     * Request user authentication for each use of the banking token.
     * The user must enter device credentials (pin, pattern, password, biometric).
     *
     * The user has chosen to use this extra layer of protection and may disable it at any time in
     * the future.
     */
    ENABLED_AUTH_PROMPT,

    /**
     * Request user authentication for each use of the banking token.
     * The user must enter device credentials (pin, pattern, password, biometric).
     *
     * In contrast to {@link #ENABLED_AUTH_PROMPT} it is not possible to disable this behavior.
     */
    MANDATORY_AUTH_PROMPT

}

/*
 * Copyright (c) 2019-2020 EFDIS AG Bankensoftware, Freising <info@efdis.de>.
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

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * This DAO is used for low level access to the database. High level business logic shall use the
 * {@link BankingTokenRepository} instead.
 */
@Dao
interface BankingTokenDao {

    @Query("select * from banking_token order by last_used desc")
    List<BankingToken> getAll();

    @Query("select * from banking_token where id = :id limit 1")
    BankingToken findById(String id);

    @Insert
    void insert(BankingToken bankingToken);

    @Update
    void update(BankingToken bankingToken);

    @Delete
    void delete(BankingToken bankingToken);

}

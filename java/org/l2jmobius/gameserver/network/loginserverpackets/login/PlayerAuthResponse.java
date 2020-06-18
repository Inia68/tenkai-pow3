/*
 * This file is part of the L2J Mobius project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.loginserverpackets.login;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.BaseRecievePacket;
import org.l2jmobius.loginserver.model.data.AccountInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

/**
 * @author -Wooden-
 */
public class PlayerAuthResponse extends BaseRecievePacket {
    private String _account;
    private boolean _authed;
    private String _token;

    /**
     * @param decrypt
     */
    public PlayerAuthResponse(byte[] decrypt) {
        super(decrypt);

        _token = readS();
        _authed = true;

    }

    /**
     * @return Returns the account.
     */
    public String getAccount() {
        return _account;
    }

    public String getToken() {
        return _token;
    }

    /**
     * @return Returns the authed state.
     */
    public boolean isAuthed() {
        return _authed;
    }
}
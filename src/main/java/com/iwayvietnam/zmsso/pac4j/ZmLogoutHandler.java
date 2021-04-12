/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zm SSO is the Zimbra Collaboration Open Source Edition extension for single sign-on authentication to the Zimbra Web Client.
 * Copyright (C) 2020-present iWay Vietnam and/or its affiliates. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 * ***** END LICENSE BLOCK *****
 *
 * Zimbra Single Sign On
 *
 * Written by Nguyen Van Nguyen <nguyennv1981@gmail.com>
 */
package com.iwayvietnam.zmsso.pac4j;

import com.iwayvietnam.zmsso.db.DbSsoSession;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraCookie;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.util.AuthUtil;
import org.pac4j.core.context.JEEContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.logout.handler.DefaultLogoutHandler;
import org.pac4j.core.logout.handler.LogoutHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Pac4j Logout Handler
 * @author Nguyen Van Nguyen <nguyennv1981@gmail.com>
 * Logout url:  https://mail.zimbra-server.com/?loginOp=logout
 */
public final class ZmLogoutHandler extends DefaultLogoutHandler implements LogoutHandler {
    private static final Provisioning prov = Provisioning.getInstance();
    private static final String X_ORIGINATING_IP_HEADER = "X-Forwarded-For";
    private static final String USER_AGENT_HEADER = "User-Agent";

    /**
     * Associates a key with the current web session.
     * @param context the web context
     * @param key the key
     */
    @Override
    public void recordSession(final WebContext context, final SessionStore sessionStore, final String key) {
        super.recordSession(context, sessionStore, key);
        getProfileManager(context, sessionStore).getProfile().ifPresent(profile -> {
            try {
                singleLogin(context, profile.getUsername(), key, profile.getClientName());
            } catch (final ServiceException e) {
                ZimbraLog.extensions.error(e);
            }
        });
    }

    /**
     * Destroys the current web session for the given key for a front channel logout.
     * @param context the web context
     * @param key the key
     */
    @Override
    public void destroySessionFront(final WebContext context, final SessionStore sessionStore, final String key) {
        try {
            clearAuthToken(context, key);
        } catch (final ServiceException e) {
            ZimbraLog.extensions.error(e);
        }
        super.destroySessionFront(context, sessionStore, key);
    }

    /**
     * Destroys the current web session for the given key for a back channel logout.
     * @param context the web context
     * @param key the key
     */
    @Override
    public void destroySessionBack(WebContext context, SessionStore sessionStore, String key) {
        try {
            singleLogout(key);
        } catch (final ServiceException e) {
            ZimbraLog.extensions.error(e);
        }
        super.destroySessionBack(context, sessionStore, key);
    }

    private void singleLogin(final WebContext context, final String accountName, final String key, final String client) throws ServiceException {
        final Map<String, Object> authCtxt = new HashMap<>();
        final String remoteIp = context.getRemoteAddr();
        final String origIp = context.getRequestHeader(X_ORIGINATING_IP_HEADER).orElse(remoteIp);
        final String userAgent = context.getRequestHeader(USER_AGENT_HEADER).orElse(null);

        authCtxt.put(AuthContext.AC_ORIGINATING_CLIENT_IP, origIp);
        authCtxt.put(AuthContext.AC_REMOTE_IP, remoteIp);
        authCtxt.put(AuthContext.AC_ACCOUNT_NAME_PASSEDIN, accountName);
        authCtxt.put(AuthContext.AC_USER_AGENT, userAgent);

        final Account account = prov.getAccountByName(accountName);
        prov.ssoAuthAccount(account, AuthContext.Protocol.soap, authCtxt);
        final AuthToken authToken = AuthProvider.getAuthToken(account, false);
        setAuthTokenCookie(context, authToken);

        DbSsoSession.ssoSessionLogin(account, key, client, origIp, remoteIp, userAgent);
    }

    private void setAuthTokenCookie(final WebContext context, final AuthToken authToken) throws ServiceException {
        if (context instanceof JEEContext) {
            final boolean isAdmin = AuthToken.isAnyAdmin(authToken);
            final JEEContext jeeCxt = (JEEContext) context;
            authToken.encode(jeeCxt.getNativeResponse(), isAdmin, context.isSecure());
            ZimbraLog.extensions.debug(String.format("Set auth token cookie for account id: %s", authToken.getAccountId()));
        }
    }

    private void clearAuthToken(final WebContext context, final String key) throws ServiceException {
        if (context instanceof JEEContext) {
            final JEEContext jeeCxt = (JEEContext) context;
            final AuthToken authToken = AuthUtil.getAuthTokenFromHttpReq(jeeCxt.getNativeRequest(), false);
            final Optional<AuthToken> optional = Optional.ofNullable(authToken);
            if (optional.isPresent()) {
                authToken.encode(jeeCxt.getNativeRequest(), jeeCxt.getNativeResponse(), true);
                try {
                    authToken.deRegister();
                } catch (final AuthTokenException e) {
                    throw ServiceException.FAILURE(e.getMessage(), e);
                }
            }
            ZimbraCookie.clearCookie(jeeCxt.getNativeResponse(), ZimbraCookie.COOKIE_ZM_AUTH_TOKEN);
            final String accountId = DbSsoSession.ssoSessionLogout(key);
            ZimbraLog.extensions.debug(String.format("SSO session logout for account id: %s", accountId));
        }
    }

    private void singleLogout(final String key) throws ServiceException {
        final String accountId = DbSsoSession.ssoSessionLogout(key);
        if (!StringUtil.isNullOrEmpty(accountId)) {
            ZimbraLog.extensions.debug(String.format("SSO single logout for account id: %s", accountId));
            final Account account = prov.getAccountById(accountId);
            final int validityValue = account.getAuthTokenValidityValue();
            if (validityValue > 99) {
                account.setAuthTokenValidityValue(1);
            } else {
                account.setAuthTokenValidityValue(validityValue + 1);
            }
        }
    }
}

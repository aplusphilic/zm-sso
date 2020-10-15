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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.config.client.PropertiesConfigFactory;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.logout.handler.LogoutHandler;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Pac4j Settings Builder
 * @author Nguyen Van Nguyen <nguyennv1981@gmail.com>
 */
public final class SettingsBuilder {
    private static final String ZM_SSO_SETTINGS_FILE = "sso.pac4j.properties";
    private static final String ZM_SSO_DEFAULT_CLIENT = "sso.defaultClient";
    private static final String ZM_SSO_CALLBACK_URL = "sso.callbackUrl";

    private static final String ZM_SSO_SAVE_IN_SESSION = "sso.saveInSession";
    private static final String ZM_SSO_MULTI_PROFILE = "sso.multiProfile";
    private static final String ZM_SSO_RENEW_SESSION = "sso.renewSession";

    private static final String ZM_SSO_LOCAL_LOGOUT = "sso.localLogout";
    private static final String ZM_SSO_DESTROY_SESSION = "sso.destroySession";
    private static final String ZM_SSO_CENTRAL_LOGOUT = "sso.centralLogout";

    private static final String ZM_SSO_SAML_AUTHN_REQUEST_SIGNED = "saml.authnRequestSigned";
    private static final String ZM_SSO_SAML_SP_LOGOUT_REQUEST_SIGNED = "saml.spLogoutRequestSigned";
    private static final String ZM_SSO_SAML_SP_METADATA_GENERATION = "saml.spMetadataGeneration";
    private static final String ZM_SSO_SAML_SP_KEYSTORE_GENERATION = "saml.spKeystoreGeneration";

    private static final String ZM_SSO_OIDC_WITH_STATE = "oidc.withState";

    private static final Map<String, String> properties = new HashMap<>();
    private static final Config config;
    private static final Optional<Client> defaultClient;

    private static final Boolean saveInSession;
    private static final Boolean multiProfile;
    private static final Boolean renewSession;

    private static final Boolean localLogout;
    private static final Boolean destroySession;
    private static final Boolean centralLogout;

    static {
        loadProperties();
        config = buildConfig();

        saveInSession = loadBooleanProperty(ZM_SSO_SAVE_IN_SESSION);
        multiProfile = loadBooleanProperty(ZM_SSO_MULTI_PROFILE);
        renewSession = loadBooleanProperty(ZM_SSO_RENEW_SESSION);

        localLogout = loadBooleanProperty(ZM_SSO_LOCAL_LOGOUT);
        destroySession = loadBooleanProperty(ZM_SSO_DESTROY_SESSION);
        centralLogout = loadBooleanProperty(ZM_SSO_CENTRAL_LOGOUT);

        defaultClient = config.getClients().findClient(loadStringProperty(ZM_SSO_DEFAULT_CLIENT));
    }

    public static Config getConfig() {
        return config;
    }

    public static Client defaultClient() throws ServiceException {
        return defaultClient.orElseThrow(() -> ServiceException.NOT_FOUND("No default client found"));
    }

    public static Boolean saveInSession() {
        return saveInSession;
    }

    public static Boolean multiProfile() {
        return multiProfile;
    }

    public static Boolean renewSession() {
        return renewSession;
    }

    public static Boolean localLogout() {
        return localLogout;
    }

    public static Boolean destroySession() {
        return destroySession;
    }

    public static Boolean centralLogout() {
        return centralLogout;
    }

    private static Config buildConfig() {
        final LogoutHandler<WebContext> logoutHandler = new ZmLogoutHandler<>();
        final PropertiesConfigFactory factory = new PropertiesConfigFactory(loadStringProperty(ZM_SSO_CALLBACK_URL), properties);
        final Config config = factory.build();

        config.getClients().findClient(CasClient.class).ifPresent(client -> {
            final CasConfiguration cfg = client.getConfiguration();
            cfg.setLogoutHandler(logoutHandler);
        });
        config.getClients().findClient(OidcClient.class).ifPresent(client -> {
            final OidcConfiguration cfg = client.getConfiguration();
            cfg.setLogoutHandler(logoutHandler);
            cfg.setWithState(loadBooleanProperty(ZM_SSO_OIDC_WITH_STATE));
        });
        config.getClients().findClient(SAML2Client.class).ifPresent(client -> {
            final SAML2Configuration cfg = client.getConfiguration();
            cfg.setLogoutHandler(logoutHandler);
            cfg.setAuthnRequestSigned(loadBooleanProperty(ZM_SSO_SAML_AUTHN_REQUEST_SIGNED));
            cfg.setSpLogoutRequestSigned(loadBooleanProperty(ZM_SSO_SAML_SP_LOGOUT_REQUEST_SIGNED));
            cfg.setForceServiceProviderMetadataGeneration(loadBooleanProperty(ZM_SSO_SAML_SP_METADATA_GENERATION));
            cfg.setForceKeystoreGeneration(loadBooleanProperty(ZM_SSO_SAML_SP_KEYSTORE_GENERATION));
        });
        return config;
    }

    private static void loadProperties() {
        final ClassLoader classLoader = SettingsBuilder.class.getClassLoader();
        final Properties prop = new Properties();
        final InputStream inputStream = classLoader.getResourceAsStream(ZM_SSO_SETTINGS_FILE);
        try {
            prop.load(inputStream);
            for (String key: prop.stringPropertyNames()) {
                properties.put(key, prop.getProperty(key));
            }
        } catch (IOException e) {
            ZimbraLog.extensions.error(e);
        }
    }

    private static String loadStringProperty(final String key) {
        return properties.get(key);
    }

    private static Boolean loadBooleanProperty(final String key) {
        final String value = properties.get(key);
        if (!StringUtil.isNullOrEmpty(value)) {
            return Boolean.parseBoolean((value).trim());
        }
        return false;
    }
}

Zimbra Single Sign On (Zm SSO)
=========================================
Zm SSO is the Zimbra Collaboration Open Source Edition extension for single sign-on authentication to the Zimbra Web Client.  
Copyright (C) 2020-present iWay Vietnam and/or its affiliates. All rights reserved.

* Using framework: [pac4j](https://www.pac4j.org) is an easy and powerful security engine for Java to authenticate users,
get their profiles and manage authorizations in order to secure web applications and web services.
* Supported authentication mechanisms: [SAML](http://www.pac4j.org/docs/clients/saml.html) -
[CAS](http://www.pac4j.org/docs/clients/cas.html) -
[OpenID Connect](http://www.pac4j.org/docs/clients/openid-connect.html)

## Building Java extension
### Requirement
* JDK 11 or newer.
* Apache Maven 3.5 or newer for Maven build.
* Apache Ant 1.x for Ant build.
* rpmbuild for rpm package build.

### Setting up your build system
* On Fedora or CentOS 8.x or Red Hat EL 7.x
```shell script
dnf -y install java-11-openjdk java-11-openjdk-devel maven ant git rpmbuild
```
* On CentOS 7.x or Red Hat EL 7.x
```shell script
yum -y install java-11-openjdk java-11-openjdk-devel maven ant git rpmbuild
```
* On Debian or Ubuntu
```shell script
apt install -y openjdk-11-jdk maven ant git
```

### Clone code from git repository
```shell script
mkdir -p ~/projects/zimbra
cd ~/projects/zimbra
git clone git@gitlab.com:iway/zm-sso.git
```

### Build jar file by using Maven
```shell script
cd ~/projects/zimbra/zm-sso
mvn clean package
```
The output should be like this:
```
--- maven-jar-plugin:3.0.2:jar (default-jar) @ zm-hab ---
Building jar: ~/projects/zimbra/zm-sso/target/zm-sso-1.0.0-1.jar
```

### Build jar file by using Ant
```shell script
cd ~/projects/zimbra/zm-sso
ant jar
```
The output should be like this:
```
[jar] Building jar: ~/projects/zimbra/zm-sso/dist/zm-sso-1.0.0-1.jar
```

### Build rpm package
```shell script
cd ~/projects/zimbra/zm-sso
make rpmbuild
```
The output should be like this:
```
Wrote: ~/rpmbuild/RPMS/noarch/zm-sso-1.0.0-1.el7.noarch.rpm
```

## Installation
### Install jar extension
* Copy jar extension to zimbra server
```shell script
cd ~/projects/zimbra/zm-sso
ssh root@zimbra.server "mkdir -p /opt/zimbra/lib/ext/zm-sso"
scp target/*.jar root@zimbra.server:/opt/zimbra/lib/ext/zm-sso
scp target/dependency/*.jar root@zimbra.server:/opt/zimbra/lib/ext/zm-sso
scp conf/zm.sso.properties root@zimbra.server:/opt/zimbra/conf
```
* Restart mailbox to load the extension.
```shell script
ssh root@zimbra.server "su - zimbra -c '/opt/zimbra/bin/zmmailboxdctl restart'"
```

### Install rpm package
```shell script
ssh root@zimbra.server "mkdir -p /tmp/zimbra"
scp ~/rpmbuild/RPMS/noarch/zm-sso-1.0.0-1.el7.noarch.rpm root@zimbra.server:/tmp/zimbra
ssh root@zimbra.server "rpm -Uvh /tmp/zimbra/zm-sso-1.0.0-1.el7.noarch.rpm"
```

## Configuration
The settings loaded from the following places:
1. An optional **zm.sso.properties** file. The default location of this file is **/opt/zimbra/conf/zm.sso.properties**
2. But it can be overridden by setting via the **localconfig.xml** file.

### Default client configuration
* Using a text editor to open **zm.sso.properties** in **/opt/zimbra/conf**. Ex: `vi /opt/zimbra/conf/zm.sso.properties`
* Specify default pac4j client by setting the value for the **sso.defaultClient** key. Ex: `sso.defaultClient = SAML2Client`
* Or execute following command with the Zimbra user: `zmlocalconfig -e sso.defaultClient=SAML2Client`

### Callback endpoint configuration
To handle authentication, a callback endpoint is necessary to receive callback calls from the identity server and finish the login process.

**Config**:
* Using a text editor to open **zm.sso.properties** in **/opt/zimbra/conf**.
* Specify callback endpoint by setting the value for the **sso.callbackUrl** key. The path of endpoint must be **/service/extension/sso/calback**. Ex: `sso.callbackUrl = https://mail.zimbra-server.com/service/extension/sso/calback`
* Specify the session must be renewed by setting the value for the **sso.renewSession** key.
* Or execute following commands with the Zimbra user:
```shell script
zmlocalconfig -e sso.callbackUrl=https://mail.zimbra-server.com/service/extension/sso/calback
zmlocalconfig -e sso.renewSession=true
```

### Logout endpoint configuration
To handle the logout, a logout endpoint is necessary to perform:
* The local logout by removing the pac4j profiles from the session.
* The central logout by calling the identity provider logout endpoint. This is the Single-Log-Out (SLO) process.

**Config**:
* Using a text editor to open **zm.sso.properties** in **/opt/zimbra/conf**.
* **sso.localLogout**: It indicates whether a local logout must be performed.
* **sso.destroySession**: It defines whether we must destroy the web session during the local logout.
* **sso.centralLogout**: It defines whether a central logout must be performed.
* Or execute following commands with the Zimbra user:
```shell script
zmlocalconfig -e sso.localLogout=true
zmlocalconfig -e sso.destroySession=true
zmlocalconfig -e sso.centralLogout=true
```

### Configuration with any SAML identity provider using the SAML v2.0 protocol.
**First**, if you don’t have one, you need to generate a keystore for all signature and encryption operations. Ex:
```shell script
keytool -genkeypair -alias saml-key-demo -keypass saml-key-passwd -keystore /opt/zimbra/conf/samlKeystore.jks -storepass saml-store-passwd -keyalg RSA -keysize 2048 -validity 3650
```

**Config**:
* Using a text editor to open **zm.sso.properties** in **/opt/zimbra/conf**.
* **saml.keystorePath**: It defines the keystore resource location. It is the value of the -keystore option for the keystore generation with prefix **file:**. Ex: `saml.keystorePath = file:/opt/zimbra/conf/samlKeystore.jks`
* **saml.keystorePassword**: It defines keystore password. It is the value of the -storepass option for the keystore generation.
* **saml.privateKeyPassword**: It defines key password. It is the value of the -keypass option for the keystore generation.
* **saml.keystoreAlias**: It defines keystore alias. It is the value of the -alias option for the keystore generation.
* **saml.identityProviderMetadataPath**: It defines the resource location should point to your IdP metadata. Ex: `saml.identityProviderMetadataPath = https://samltest.id/saml/idp`
* **saml.serviceProviderEntityId**: It defines the entity ID of your application (the Service Provider). Ex: `saml.serviceProviderEntityId = https://mail.zimbra-server.com/service/extension/saml/metadata`
* Or execute following commands with the Zimbra user:
```shell script
zmlocalconfig -e saml.keystorePath=file:/opt/zimbra/conf/samlKeystore.jks
zmlocalconfig -e saml.keystorePassword=saml-store-passwd
zmlocalconfig -e saml.privateKeyPassword=saml-key-passwd
zmlocalconfig -e saml.keystoreAlias=saml-key-demo
zmlocalconfig -e saml.identityProviderMetadataPath=https://samltest.id/saml/idp
zmlocalconfig -e saml.serviceProviderEntityId=https://mail.zimbra-server.com/service/extension/saml/metadata
```

### Configuration to login with a CAS server.
* Using a text editor to open **zm.sso.properties** in **/opt/zimbra/conf**.
* **cas.loginUrl**: It defines the login URL of your CAS server. Ex: `cas.loginUrl = https://casserver.herokuapp.com/cas/login`
* **cas.protocol**: It defines the CAS protocol you want to use. Ex: `cas.protocol = CAS20`
* Or execute following commands with the Zimbra user:
```shell script
zmlocalconfig -e cas.loginUrl=https://casserver.herokuapp.com/cas/login
zmlocalconfig -e cas.protocol=CAS20
```

### Configuration to login using the OpenID Connect protocol v1.0.
* Using a text editor to open **zm.sso.properties** in **/opt/zimbra/conf**.
* **oidc.discoveryUri**: It defines the discovery URI for fetching OP metadata. Ex: `oidc.discoveryUri = https://demo.c2id.com/.well-known/openid-configuration`
* **oidc.id**: It defines the OpenID client identifier.
* **oidc.secret**: It defines the OpenID client secret.
* **oidc.scope**: It defines the OpenID client scope.
* Or execute following commands with the Zimbra user:
```shell script
zmlocalconfig -e oidc.discoveryUri=https://demo.c2id.com/.well-known/openid-configuration
zmlocalconfig -e oidc.id=000123
zmlocalconfig -e oidc.secret=rlC_8s3oBayCynAO_7UKt34hbEwiiTKx0l7zRcrFY3A
zmlocalconfig -e oidc.scope=openid email profile
```

### Replace login and logout urls
* Execute following commands with the Zimbra user for domain configuration:
```shell script
zmprov md yourdomain.com zimbraWebClientLoginURL https://mail.zimbra-server.com/service/extension/sso/login
zmprov md yourdomain.com zimbraWebClientLogoutURL https://mail.zimbra-server.com/service/extension/sso/logout
```
* Execute following commands with the Zimbra user for global configuration:
```shell script
zmprov mcf zimbraWebClientLoginURL https://mail.zimbra-server.com/service/extension/sso/login
zmprov mcf zimbraWebClientLogoutURL https://mail.zimbra-server.com/service/extension/sso/logout
```
* Execute the following command with the Zimbra user to restart Zimbra server: `zmcontrol restart`

Licensing
=========
Zm SSO is licensed under the GNU AFFERO GENERAL PUBLIC LICENSE, Version 3.
See [LICENSE](LICENSE) for the full license text.

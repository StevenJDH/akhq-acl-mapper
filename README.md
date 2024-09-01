# AKHQ ACL Mapper for Keycloak

![GitHub release (latest by date including pre-releases)](https://img.shields.io/github/v/release/StevenJDH/akhq-acl-mapper?include_prereleases)
![GitHub All Releases](https://img.shields.io/github/downloads/StevenJDH/akhq-acl-mapper/total)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/7bef58acd7cd45bdb37a417c7534578e)](https://app.codacy.com/gh/StevenJDH/akhq-acl-mapper/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
![Maintenance](https://img.shields.io/badge/yes-4FCA21?label=maintained&style=flat)
![GitHub](https://img.shields.io/github/license/StevenJDH/akhq-acl-mapper)

AKHQ ACL Mapper is a custom protocol mapper for Keycloak that supports AKHQ's latest ACL requirements as of version 0.25.0 when using [Direct OIDC mapping](https://akhq.io/docs/configuration/authentifications/oidc.html#direct-oidc-mapping). This mapper is only meant to be used as a simple way to transition from previous AKHQ versions for simple setups that used group attributes to adapt the UI to a logged in user. Because of this, many features like multi-cluster support are not supported, but the script may be useful as a base to adapt to any other needs.

[![Buy me a coffee](https://img.shields.io/static/v1?label=Buy%20me%20a&message=coffee&color=important&style=flat&logo=buy-me-a-coffee&logoColor=white)](https://www.buymeacoffee.com/stevenjdh)

## Features
* Maps previous `topics-filter-regexp`, `connects-filter-regexp`, `consumer-groups-filter-regexp` attributes to new ACLs.
* Avoids use of `*-writer` roles to prevent users with `topics/create` or `connect/create` roles from creating disallowed resources.
* Automatically creates the parent `groups` claim for the ACL substructure.
* Basic debugging support.

## Prerequisites
* [Keycloak](https://www.keycloak.org/downloads) 25.0.0 or newer (20.0.0+ may also work).
* [AKHQ](https://github.com/tchiotludo/akhq/releases) 0.25.0 or newer.

## Building
To build the JAR file, zip together the `META-INF` folder and the `akhq-acl-mapper.js` file together. After, change the `.zip` extension to the `.jar` extension. For more information, see [Create a JAR with the scripts to deploy](https://www.keycloak.org/docs/latest/server_development/#create-a-jar-with-the-scripts-to-deploy).

## Usage
The following describes what is needed to get up and running with this mapper.

### Install custom provider for traditional setups
When using a traditional setup, place the `akhq-acl-mapper.jar` file into the `providers` folder of Keycloak. Restart the Keycloak server with the following command:

```bash
bin/kc.[sh|bat] start-dev --features=scripts
```

**Note:** This command example starts Keycloak in development mode for testing only. The command also enables the scripts preview feature which is required by the custom mapper.

### Install custom provider for Kubernetes setups
When using a Kubernetes setup with Keycloak installed via the Bitnami Helm Chart, modify the chart´s `values.yaml` file to include the following configuration:

```yaml
initdbScripts:
  load_custom_provider_script.sh: |
    #!/bin/bash
    echo "load_custom_provider_script.sh"
    curl -sS https://github.com/StevenJDH/akhq-acl-mapper/releases/download/0.1.0/akhq-acl-mapper.jar -o /opt/bitnami/keycloak/providers/akhq_acl_mapper.jar

extraEnvVars:
  - name: KEYCLOAK_EXTRA_ARGS
    value: "--features=scripts"
```

**Note:** This configuration enables the scripts preview feature which is required by the custom mapper.

### Configure user group attributes
Ensure that the user group attributes match the `topics-filter-regexp`, `connects-filter-regexp`, `consumer-groups-filter-regexp` keys. If they don't, then they will either need to be updated or the script adjusted to match.

### Add custom protocol mapper
In Keycloak, perform the following steps:

1. Go to `Clients`, select the client used for AKHQ, and navigate to the `Client scopes` tab.
2. Select the `<akhq>-dedicated` item in the list.
3. Click the `Configure a new mapper` button, and select the `AKHQ ACL Mapper` type in the list that appears.
4. Configure the mapper as follows:
   * **Name:** akhq-acl-mapper
   * **Token Claim Name:** (leave blank as it will be set by the mapper)
   * **Claim JSON Type:** JSON
   * **Add to ID token:** On
   * **Add to access token:** Off

### Testing the mapper
Under the `Client scopes` tab of the AKHQ client configuration, select the `Evaluate` sub-tab. In the `Users` field, select a user associated with a group that has attributes configured for AKHQ. Then, on the right, click the `Generated ID token` button. There should now be a groups claim in the generated token on the left that has the required ACL structure similar to the following:

```json
{
  "exp": 1725228553,
  "iat": 1725228493,
  "jti": "2fd02132-646f-47a4-8059-35e45568d06b",
  "iss": "http://localhost:8080/realms/master",
  "aud": "akhq",
  "sub": "a123346f-a37e-4369-807f-b313308a7ef4",
  "typ": "ID",
  "azp": "akhq",
  "sid": "bfc80bf8-6c10-46cb-8906-cab245b886e3",
  "acr": "1",
  "email_verified": true,
  "name": "john doe",
  "groups": {
    "project-x": [
      {
        "role": "topic-reader",
        "patterns": [
          "test.*"
        ]
      },
      {
        "role": "group-reader",
        "patterns": [
          ".*"
        ]
      },
      {
        "role": "connect-reader",
        "patterns": [
          ".*"
        ]
      }
    ]
  },
  "preferred_username": "j.doe",
  "given_name": "john",
  "family_name": "doe",
  "email": "j.doe@example.com"
}
```

## Contributing
Thanks for your interest in contributing! There are many ways to contribute to this project. Get started [here](https://github.com/StevenJDH/.github/blob/main/docs/CONTRIBUTING.md).

## Do you have any questions?
Many commonly asked questions are answered in the FAQ:
[https://github.com/StevenJDH/akhq-acl-mapper/wiki/FAQ](https://github.com/StevenJDH/akhq-acl-mapper/wiki/FAQ)

## Want to show your support?

|Method          | Address                                                                                   |
|---------------:|:------------------------------------------------------------------------------------------|
|PayPal:         | [https://www.paypal.me/stevenjdh](https://www.paypal.me/stevenjdh "Steven's Paypal Page") |
|Cryptocurrency: | [Supported options](https://github.com/StevenJDH/StevenJDH/wiki/Donate-Cryptocurrency)    |


// Steven Jenkins De Haro ("StevenJDH" on GitHub)

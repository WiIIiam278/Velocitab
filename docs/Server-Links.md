> **Note:** This feature will only apply for users connecting with **Minecraft 1.21+** clients

Velocitab supports sending _Server Links_ to players, which will be displayed in the player pause menu by 1.21+ game clients. This can be useful for linking to your server's website, Discord, or other resources.

## Configuring
Server links are configured with the `server_links` section in your `config.yml` file. A link must have:

* A `url` field; a valid web URL to link to
* A `label` field, which is the text to display for the link. Labels can be::
  * Fully formatted custom text. You may include placeholders and formatting valid for your chosen formatter.
  * One of the following built-in label strings, which will be localized into the user's client language:
    * `bug_report` - Will also be shown on the disconnection error screen.
    * `community_guidelines`
    * `support`
    * `status`
    * `feedback`
    * `community`
    * `website`
    * `forums`
    * `news`
    * `announcements`
* A `groups` field, which is a list of server groups the link should be sent to connecting players on.
  * Use `'*'` to show the link to all groups.

### Example section
```yaml
server_links:
  - url: 'https://william278.net/project/velocitab'
    label: 'website'
    groups: ['*']
  - url: 'https://william278.net/docs/velocitab'
    label: 'Documentation'
    groups: ['*']
  - url: 'https://github.com/William278/Velocitab/issues'
    label: 'bug_report' # This will use the bug report built-in label and also be shown on the player disconnect screen
    groups: ['*']
```
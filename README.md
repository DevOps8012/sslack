# SSlack #

[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## Demo ##

- Access our [demo site](http://sslack.herokuapp.com/) on Heroku.
- Input "王" to `query`.
- You see two messages from king and queen.
- Let's add user constraint. Input "king" to `user`.
- You get only one message from king.

Demo is in Japanese in order to show that SSlack can handle chat between Japanese members.
It also works for English.
If SSlack misbehaves in your language, please report an issue or send a pull-request.

## Install ##

- Setup [elasticsearch](http://www.elasticsearch.org/)
- Install [kuromoji](https://github.com/elasticsearch/elasticsearch-analysis-kuromoji)

```
bin/plugin -install elasticsearch/elasticsearch-analysis-kuromoji/2.1.0
```

## Build & Run ##

```sh
$ ./sbt
> container:start
> browse
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.

To try searching without chatting in Slack many, use bundled `import.sh`.

## Slack integration ##

- Go to outgoing webhook set-up page. https://YOUR_TEAM.slack.com/services/new/outgoing-webhook

- Select the Channel you want to watch. Slack outgoing webhook works only for one channel when Trigger Word is empty.

- Put sslack URL to URLs: `http://YOUR_SSLACK_HOST/slack-webhook` .

- Then "Save Integration". All done!

## License ##

MIT License.

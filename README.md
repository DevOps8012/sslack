# SSlack #

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

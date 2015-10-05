var express    = require('express');
var app        = express();
var bodyParser = require('body-parser');
var pg = require('pg');
pg.defaults.poolSize = 100;

app.use(bodyParser.urlencoded({ extended: true }));
app.use(bodyParser.json());

var port = process.env.PORT || 8080;
var connectionString = "postgres://dropwizard:dropwizard@localhost/dropwizard";

var router = express.Router();

router.get('/', function(req, res) {
    pg.connect(connectionString, function(err, client, done) {
        var productId = req.query.id;
        if(err) {
            return console.error('error fetching client from pool', err);
        }
        client.query('select find_product_name($1::int) AS name', [productId], function(err, result) {
            done();
            if(err) {
              return console.error('error running query', err);
            }
            var productName = result.rows[0].name
            res.json({ id: productId, name: productName});
          });
        });
});

app.use('/productNodeJs', router);

app.listen(port);
console.log('Server started on port ' + port);


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import db.DBDriver;
import model.*;
import org.bson.Document;
import service.ArticleService;
import service.DataService;
import service.LikeService;
import service.UrlService;
import utils.Mail;
import utils.initializeLists;

import java.util.ArrayList;
import java.util.Iterator;

import static spark.Spark.get;
import static spark.Spark.post;

public class App {

    public static void main(String[] args) {

        initializeLists.generateLists();
        final DBDriver dbDriver = new DBDriver();
        final LikeService likeService = new LikeService(dbDriver);
        final UrlService urlService = new UrlService(dbDriver);
        final DataService dataService = new DataService(dbDriver);
        final ArticleService articleService = new ArticleService();
        final Crawler crawler = new Crawler();

        // Get datas stored in Data collection
        get("/datas", (request, response) -> {
            response.type("application/json");

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,
                    new Gson().toJsonTree(dataService.getDatas())));
        });

        // Reads each url from text file and then inserts the urls into the Url collection
        get("/urls", (request, response) -> {
            response.type("application/json");

            ArrayList<String> list = crawler.fileToList();

            for (String url : list) {
                Url link = crawler.urlToUrlCollection(url);
                urlService.addUrls(link);
            }

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS));
        });

        post("/urls", (request, response) -> {
            response.type("application/json");

            Url url = new Gson().fromJson(request.body(), Url.class);
            urlService.addUrls(url);

            return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS));
        });

        // Reads each url from database, crawls it and then
        // -> Appends each formatted data into articles.csv file
        // -> Inserts each formatted data into Data collection
        // Use /crawl for generating .csv file which contains all articles
        get("/crawl", (request, response) -> {
            response.type("application/json");

            ArrayList<String> urls = urlService.getUrlsAsList();

            for (String url : urls) {
                int articleID = counterValue(dbDriver);
                Data data = crawler.urlToData(url, articleID);
                crawler.writeDatas(data);
                dataService.addData(data);
            }

            return new Gson().toJson(
                    new StandardResponse(StatusResponse.SUCCESS,
                            new Gson().toJsonTree(dataService.getDatas())));
        });

        get("/recommend", (request, response) -> {

            response.type("application/json");

            getUrls(urlService, crawler, likeService);
            getLikesAndRecommend(likeService, articleService);
            getRecommends(articleService, dataService);

            return new Gson().toJson(
                    new StandardResponse(StatusResponse.SUCCESS,
                            new Gson().toJsonTree(urlService.getUrlsAsList())));
        });
    }

    /*
    // Create's a collection named counter if there's none.
    // Increments counterValue by 1 and returns it.
    // Purpose of this collection : Defines an articleID for each article.
     */
    public static int counterValue(DBDriver dbDriver) {

        // Will relocate this function and will remove the parameter
        MongoDatabase mongoDatabase = dbDriver.getDatabaseInstance();
        MongoCollection<Counter> collection = mongoDatabase.getCollection("counter", Counter.class);

        Document query = new Document("counterName", "articleID");
        Document update = new Document();
        Document inside = new Document();
        inside.put("counterValue", 1);
        update.put("$inc", inside);

        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();
        options.returnDocument(ReturnDocument.AFTER);
        options.upsert(true);

        Counter doc = collection.findOneAndUpdate(query, update, options);
        return doc.getCounterValue();
    }

    public static void getUrls(UrlService urlService, Crawler crawler, LikeService likeService) {
        ArrayList<String> urls = urlService.getUrlsAsList();

        for (String url : urls) {
            Like like = crawler.urlToLikeCollection(url);
            like.toString();
            crawler.writeLikes(like);
            likeService.addLike(like);
        }
    }

    public static void getLikesAndRecommend(LikeService likeService, ArticleService articleService) {

        ArrayList<Like> likes = likeService.getLikesAsList();
        Iterator<Like> iter = likes.iterator();

        while(iter.hasNext()) {
            Like like = iter.next();
            JsonObject jsonObject = articleService.getRecommendations(like.getTitle());

            // == instead of equals() maybe?
            if (like.getMainTopic().equals(MainTopics.DEVELOPMENT.getMainTopic())) {
                articleService.JsonObjectToList(jsonObject, initializeLists.development);
            } else if (like.getMainTopic().equals(MainTopics.ARCHITECTURE.getMainTopic())) {
                articleService.JsonObjectToList(jsonObject, initializeLists.architecture);
            } else if (like.getMainTopic().equals(MainTopics.AI.getMainTopic())) {
                articleService.JsonObjectToList(jsonObject, initializeLists.ai);
            } else if (like.getMainTopic().equals(MainTopics.CULTURE.getMainTopic())) {
                articleService.JsonObjectToList(jsonObject, initializeLists.culture);
            } else if (like.getMainTopic().equals(MainTopics.DEVOPS.getMainTopic())) {
                articleService.JsonObjectToList(jsonObject, initializeLists.devops);
            } else {
                articleService.JsonObjectToList(jsonObject, new ArrayList<Article>());
            }
        }
    }

    public static void getRecommends(ArticleService articleService, DataService dataService) {
        initializeLists.development = articleService.returnRecommendations(initializeLists.development);
        initializeLists.architecture = articleService.returnRecommendations(initializeLists.architecture);
        initializeLists.ai = articleService.returnRecommendations(initializeLists.ai);
        initializeLists.culture = articleService.returnRecommendations(initializeLists.culture);
        initializeLists.devops = articleService.returnRecommendations(initializeLists.devops);

        StringBuilder sb = new StringBuilder();

        sb.append(dataService.sendRecommendations(initializeLists.development));
        sb.append(dataService.sendRecommendations(initializeLists.architecture));
        sb.append(dataService.sendRecommendations(initializeLists.ai));
        sb.append(dataService.sendRecommendations(initializeLists.culture));
        sb.append(dataService.sendRecommendations(initializeLists.devops));

        Mail mail = new Mail();
        mail.sendMail(sb.toString());
    }
}
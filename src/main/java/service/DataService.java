package service;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import model.Article;
import model.Data;
import org.bson.Document;
import org.bson.conversions.Bson;
import utils.initializeDB;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;

public class DataService {

    protected final MongoDatabase database;

    public DataService() {
        database = initializeDB.getDatabase();
    }

    public void addData(Data data){
        MongoCollection<Data> collection = database.getCollection("data", Data.class);

        collection.insertOne(data);
    }

    public ArrayList<Data> getDatas() {
        MongoCollection<Data> collection = database.getCollection("data", Data.class);

        ArrayList<Data> list = new ArrayList<Data>();

        try(MongoCursor<Data> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                Data data = cursor.next();
                list.add(data);
            }
        }

        return list;
    }

    public boolean dataExist(Data data) {
        MongoCollection<Data> collection = database.getCollection("data", Data.class);

        Document queryFilter =  new Document("title", data.getTitle());

        FindIterable result = collection.find(queryFilter).limit(1);

        return result.first() == null ? false : true;
    }

    public String createMail(Data data) {
        // verilerden mail formati olusturmasini bekleriz.
        String mainTopic = data.getMainTopic();
        String title = data.getTitle();
        String url = data.getArticleLink();

        String html = "<h2> " + mainTopic + " </h2>" + "\n"
                + "<h4> " + title + " </h4>" + "\n"
                + "<h5> " + url + " </h5>";

        return html;
    }
}

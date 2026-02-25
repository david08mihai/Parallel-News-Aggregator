import java.util.ArrayList;

public class Input {
    ArrayList<String> pathToArticles;
    ArrayList<String> categories;
    ArrayList<String> languages;
    ArrayList<String> linkingWords;

    public Input(ArrayList<String> pathToArticles, ArrayList<String> categories, ArrayList<String> languages, ArrayList<String> linkingWords) {
        this.pathToArticles = pathToArticles;
        this.categories = categories;
        this.languages = languages;
        this.linkingWords = linkingWords;
    }
}

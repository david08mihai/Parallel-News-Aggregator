import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.Scanner;

public class NewsAggregator {
    static int N, P;
    static ConcurrentHashMap<String, Integer> uuidMap = new ConcurrentHashMap<String, Integer>();
    static ConcurrentHashMap<String, Integer> titleMap = new ConcurrentHashMap<String, Integer>();
    static ConcurrentHashMap<String, Boolean> uuidNotToInsert = new ConcurrentHashMap<String, Boolean>();
    static ConcurrentHashMap<String, Boolean> titleNotToInsert = new ConcurrentHashMap<String, Boolean>();
    static ConcurrentHashMap<String, List<Articles>> allCategoriesStorage = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Integer> numberOfApparitions = new ConcurrentHashMap<>();
    static AtomicInteger firstThread = new AtomicInteger(0);
    static List<Articles> allArt = new ArrayList<Articles>();
    static List<Articles> articles = new ArrayList<Articles>();
    static List<CountWords> countWords = new ArrayList<>();
    static Input input;
    static int[] toDos = new int[3];

    public static void parsingFiles(String pathFile, String auxFile) {
        String categoriesPath = "";
        String languagesPath = "";
        String linkingWordsPath = "";
        ArrayList<String> articlesPaths = new ArrayList<>();

        File mainFile = new File(pathFile);
        File parentDir = mainFile.getParentFile();
        try {
            Scanner articlesScanner = new Scanner(mainFile);
            while (articlesScanner.hasNextInt()) {
                N = articlesScanner.nextInt();
            }
            articlesScanner.nextLine();
            while (articlesScanner.hasNextLine()) {
                String line = articlesScanner.nextLine();
                File absoluteFile = new File(parentDir, line);
                articlesPaths.add(absoluteFile.getAbsolutePath());
            }
            articlesScanner.close();
        } catch (Exception e) {
            System.out.println("Path file error");
        }

        try {
            Scanner auxScanner = new Scanner(new File(auxFile));
            while (auxScanner.hasNextInt())
                auxScanner.nextInt();
            while (auxScanner.hasNextLine()) {
                String line = auxScanner.nextLine();
                if (line.contains("languages"))
                    languagesPath = line;
                else if (line.contains("categories"))
                    categoriesPath = line;
                else if (line.contains("linking")) {
                    linkingWordsPath = line;
                }
            }
            auxScanner.close();
        } catch (IOException e) {
            System.out.println("AUX file error");
        }
        ArrayList<String> categoriesList = getList(parentDir, categoriesPath);
        ArrayList<String> linkingWordsList = getList(parentDir, linkingWordsPath);
        ArrayList<String> langList = getList(parentDir, languagesPath);

        input = new Input(articlesPaths, categoriesList, langList, linkingWordsList);
    }

    private static ArrayList<String> getList(File parentDir, String languagesPath) {
        ArrayList<String> list = new ArrayList<>();
        try {
            Scanner scanner = new Scanner(new File(parentDir, languagesPath));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                list.add(line);
            }
            scanner.close();
            list.remove(0);
        } catch (IOException e) {
            System.out.println("Path file error");
        }
        return list;
    }

    public static void bindStringToList() {
        for (String category : input.categories) {
            allCategoriesStorage.put(category, new ArrayList<Articles>());
        }
        for (String language : input.languages) {
            allCategoriesStorage.put(language, new ArrayList<Articles>());
        }
        allCategoriesStorage.put("all_articles", new ArrayList<Articles>());
    }

    public static void main(String[] args) {
        P = Integer.parseInt(args[0]);
        MyThread[] threads = new MyThread[P];

        toDos[0] = 0;
        toDos[1] = 1;
        toDos[2] = 2;

        HashMap<String, Boolean> deniedWords = new HashMap<>();
        parsingFiles(args[1], args[2]);
        for (String words : input.linkingWords) {
            deniedWords.put(words, true);
        }

        bindStringToList();
        CyclicBarrier barrier = new CyclicBarrier(P);

        for (int i = 0; i < P; i++) {
            threads[i] = new MyThread(i, N, P, articles, input, uuidMap, titleMap, barrier, titleNotToInsert, uuidNotToInsert,
                    allArt, firstThread, allCategoriesStorage, deniedWords, numberOfApparitions,
                    countWords, toDos);
            threads[i].start();
        }
        for (int i = 0; i < P; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                System.out.println("Thread " + i + " interrupted");
            }
        }

    }
}
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.DeserializationFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.Math;

public class MyThread extends Thread {
    int[] toDos;
    public static AtomicInteger numberOfDuplicates = new AtomicInteger(0);
    public static String bestAuthor = "";
    public static String topLanguage = "";
    public static String topCategory = "";
    public static ConcurrentHashMap<String, Integer> globalAuthorArticles =  new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Integer> globalTopLanguage =  new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Integer> globalTopCategory =  new ConcurrentHashMap<>();
    private final int id;
    List<Articles> articles;
    private final int N;
    private final int P;
    ConcurrentHashMap<String, Integer> uuidMap;
    ConcurrentHashMap<String, Integer> titleMap;
    CyclicBarrier barrier;
    ConcurrentHashMap<String, Boolean> titleNotToInsert;
    ConcurrentHashMap<String, Boolean> uuidNotToInsert;
    final List<Articles> allArtLocker;
    AtomicInteger firstThread;
    ConcurrentHashMap<String, List<Articles>> allCategoriesStorage;
    Input input;
    HashMap<String, Boolean> deniedWords;
    ConcurrentHashMap<String, Integer> globalWordsCount;
    List<CountWords> countWords;
    public static final ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public MyThread(int id, int N, int P, List<Articles> articles, Input input, ConcurrentHashMap<String, Integer> uuidMap,
                    ConcurrentHashMap<String, Integer> titleMap, CyclicBarrier barrier,
                    ConcurrentHashMap<String, Boolean> uuidNotToInsert, ConcurrentHashMap<String, Boolean> titleNotToInsert,
                    List<Articles> allArtLocker, AtomicInteger firstThread, ConcurrentHashMap<String, List<Articles>> allCategoriesStorage,
                    HashMap<String, Boolean> deniedWords, ConcurrentHashMap<String, Integer> globalWordsCount,
                    List<CountWords> countWords, int[] toDos) {
        this.id = id;
        this.N = N;
        this.P = P;
        this.articles = articles;
        this.input = input;
        this.uuidMap = uuidMap;
        this.titleMap = titleMap;
        this.barrier = barrier;
        this.uuidNotToInsert = uuidNotToInsert;
        this.titleNotToInsert = titleNotToInsert;
        this.allArtLocker = allArtLocker;
        this.firstThread = firstThread;
        this.allCategoriesStorage = allCategoriesStorage;
        this.deniedWords = deniedWords;
        this.globalWordsCount = globalWordsCount;
        this.countWords = countWords;
        this.toDos = toDos;
    }
    @Override
    public void run() {

        // logic for each thread to process the input
        // assigned to it by checking if it was
        // duplicated or no.
        // in case the uuidMap or titleMap already contains the article,
        // it will be placed in another ConcurrentMap and next time it won't be added
        int start = (int)(id * (double) N  / P);
        int end = Math.min(N, (int) ((id + 1) * (double) N / P));

        List<Articles> threadsArticles = new ArrayList<>();
        for (int i = start ; i < end; i++) {
            String path = input.pathToArticles.get(i);
            try {
                List<Articles> localArticles = objectMapper.readValue(new File(path), new TypeReference<List<Articles>>() {});
                for (Articles article : localArticles) {
                    String uuid = article.getUuid();
                    if (uuidMap.putIfAbsent(uuid, 1) != null) {
                        uuidNotToInsert.put(uuid, true);
                    }
                    String title = article.getTitle();
                    if (titleMap.putIfAbsent(title, 1) != null) {
                        titleNotToInsert.put(title, true);
                    }
                    threadsArticles.add(article);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            System.out.println("Broken barrier before adding not duplicates");
        }

        // insert only articles that are not duplicated
        List<Articles> threadsUniqueArticles = new ArrayList<>();
        for (Articles article : threadsArticles) {
            String title = article.getTitle();
            String uuid = article.getUuid();
            if (titleNotToInsert.containsKey(title) || uuidNotToInsert.containsKey(uuid)) {
                numberOfDuplicates.incrementAndGet();
                continue;
            }
            threadsUniqueArticles.add(article);
        }

        synchronized(articles) {
            articles.addAll(threadsUniqueArticles);
        }
        // all threads should finish before computing other tasks
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            System.out.println("Barrier after adding not duplicates");
        }

        HashMap<String, Integer> localWordsCount = new HashMap<>();
        int articleSize = articles.size();
        start = (int)(id * (double) articleSize / P);
        end = Math.min(articleSize, (int) ((id + 1) * (double) articleSize / P));

        HashMap<String, Integer> localAuthorArticles = new HashMap<>();
        HashMap<String, Integer> localTopLanguage = new HashMap<>();
        HashMap<String, Integer> localTopCategory = new HashMap<>();
        HashMap<String, List<Articles>> localCategoriesStorage = new HashMap<>();
        for (String category : input.categories) {
            localCategoriesStorage.put(category, new ArrayList<>());
        }
        HashMap<String, List<Articles>> localLanguagesStorage = new HashMap<>();
        for (String language : input.languages) {
            localLanguagesStorage.put(language, new ArrayList<>());
        }
        // for each article from shared articles
        for (int i = start; i < end; ++i) {
            Articles art = articles.get(i);
            Set<String> uniqueCategories = new HashSet<>(art.getCategories());
            // add articles that belongs to a specific category to a local list
            for (String category : uniqueCategories) {
                if (allCategoriesStorage.containsKey(category)) {
                        String UUID = art.getUuid();
                        Articles toAddArticle = new Articles();
                        toAddArticle.setUuid(UUID);
                        localCategoriesStorage.get(category).add(toAddArticle);
                }
            }
            // add articles that belongs to a specific language to a local list
            String language = art.getLanguage();
            if (allCategoriesStorage.containsKey(language)) {
                    String UUID = art.getUuid();
                    Articles toAddArticle = new Articles();
                    toAddArticle.setUuid(UUID);
                    localLanguagesStorage.get(language).add(toAddArticle);
            }

            // add from each local part in hash for finding the best author, language and category
            localAuthorArticles.put(art.getAuthor().trim(), localAuthorArticles.getOrDefault(art.getAuthor(), 0) + 1);
            localTopLanguage.put(art.getLanguage().trim(), localTopLanguage.getOrDefault(art.getLanguage(), 0) + 1);
            Set<String> uniqueCategories2 = new HashSet<>(art.getCategories());
            for (String category : uniqueCategories2) {
                localTopCategory.put(category, localTopCategory.getOrDefault(category, 0) + 1);
            }

            if (art.getLanguage().equals("english")) {
                HashSet<String> uniqueWordInArticle = new HashSet<>();
                String clearedText = art.getText().toLowerCase().replaceAll("[^a-z\\s]", "");
                String tokens[] = clearedText.split("\\s+");
                for (String token : tokens) {
                    if (token.isEmpty()) continue;

                    if (!deniedWords.containsKey(token)) {
                        if (uniqueWordInArticle.add(token)) {
                            localWordsCount.put(token, localWordsCount.getOrDefault(token, 0) + 1);
                        }
                    }
                }
            }
        }

        // add in a shared list the categories
        for (Map.Entry<String, List<Articles>> entry : localCategoriesStorage.entrySet()) {
            if (allCategoriesStorage.containsKey(entry.getKey())) {
                synchronized (allCategoriesStorage.get(entry.getKey())) {
                    allCategoriesStorage.get(entry.getKey()).addAll(entry.getValue());
                }
            }
        }

        // add in a shared list the languages
        for (Map.Entry<String, List<Articles>> entry : localLanguagesStorage.entrySet()) {
            if  (allCategoriesStorage.containsKey(entry.getKey())) {
                synchronized (allCategoriesStorage.get(entry.getKey())) {
                    allCategoriesStorage.get(entry.getKey()).addAll(entry.getValue());
                }
            }
        }


        // after all processes have done, I will put the total count in the shared hashmap
        combineLocalToShared(localWordsCount, globalWordsCount);
        combineLocalToShared(localAuthorArticles, globalAuthorArticles);
        combineLocalToShared(localTopLanguage, globalTopLanguage);
        combineLocalToShared(localTopCategory, globalTopCategory);

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            System.out.println("Barrier after combination of local to shared");
        }

        // explore all articles and find the best of them
        start = (int)(id * (double) 3  / P);
        end = Math.min(3, (int) ((id + 1) * (double) 3 / P));
        for (int i = start; i < end; ++i) {
            if (toDos[i] == 0) {
                bestAuthor = getBestOf(globalAuthorArticles, 0);
            } else if (toDos[i] == 1) {
                topCategory = getBestOf(globalTopCategory, 1);
            } else if (toDos[i] == 2) {
                topLanguage = getBestOf(globalTopLanguage, 0);
            }
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        // local array to know which thread is assigned to sort an array
        // and then to write it in files
        ArrayList<String> toBeWritten = new ArrayList<>();
        toBeWritten.addAll(input.categories);
        toBeWritten.addAll(input.languages);
        toBeWritten.add("key_words");
        toBeWritten.add("all_articles");
        toBeWritten.add("reports");

        int arraysToBeWritten = toBeWritten.size();
        start = (int)(id * (double) arraysToBeWritten  / P);
        end = Math.min(arraysToBeWritten, (int) ((id + 1) * (double) arraysToBeWritten / P));

        for (int i = start; i < end; ++i) {
            String assignedArray = toBeWritten.get(i);
            List<Articles> localArray = allCategoriesStorage.get(assignedArray);
            if (!assignedArray.equals("all_articles") && localArray != null && !localArray.isEmpty()) {
                localArray.sort(Comparator.comparing(Articles::getUuid));
            } else if (assignedArray.equals("all_articles")) {
                articles.sort(Comparator.comparing(Articles::getPublished, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(Articles::getUuid, Comparator.nullsLast(Comparator.naturalOrder())));
            } else if (assignedArray.equals("key_words")) {
                for (Map.Entry<String, Integer> entry : globalWordsCount.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    countWords.add(new CountWords(key, value));
                }
                Collections.sort(countWords);
            }
        }

        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }

        for (int i = start; i < end; ++i) {
            String assignedArray = toBeWritten.get(i);
            List<Articles> localArray = allCategoriesStorage.get(assignedArray);
            if (!assignedArray.equals("all_articles") && localArray != null && !localArray.isEmpty()) {
                String fileName = getFileName(assignedArray);
                String pathToFile = fileName + ".txt";
                BufferedWriter bf;
                try {
                    bf = new BufferedWriter(new FileWriter(pathToFile));
                    StringBuilder buffer = new StringBuilder();
                    int count = 0;
                    for (Articles art : localArray) {
                        buffer.append(art.getUuid()).append("\n");
                        count++;
                        if (count % 500 == 0) {
                            bf.write(buffer.toString());
                            buffer.delete(0, buffer.length());
                        }
                    }
                    if (!buffer.isEmpty()) {
                        bf.write(buffer.toString());
                    }
                    bf.flush();
                    bf.close();
                } catch (IOException e) {
                    System.out.println("Error writing file " + pathToFile);
                }
            } else if (assignedArray.equals("all_articles")) {
                BufferedWriter bf;
                String pathToFile = "all_articles.txt";
                try {
                    bf = new BufferedWriter(new FileWriter(pathToFile));
                    StringBuilder buffer = new StringBuilder();
                    int count = 0;
                    for (Articles art : articles) {
                        String toWrite = art.getUuid() + " " + art.getPublished();
                        buffer.append(toWrite).append("\n");
                        count++;
                        if (count % 500 == 0) {
                            bf.write(buffer.toString());
                            buffer.delete(0, buffer.length());
                        }
                    }
                    if (!buffer.isEmpty()) {
                        bf.write(buffer.toString());
                    }
                    bf.flush();
                    bf.close();
                } catch (IOException e) {
                    System.out.println("Error writing file " + pathToFile);
                }
            } else if (assignedArray.equals("reports")) {
                BufferedWriter bf;
                String pathToFile = "reports.txt";
                try {
                    bf = new BufferedWriter(new FileWriter(pathToFile));
                    bf.write("duplicates_found - " + numberOfDuplicates);
                    bf.newLine();
                    bf.write("unique_articles - " + articles.size());
                    bf.newLine();
                    bf.write("best_author - " + bestAuthor);
                    bf.newLine();
                    bf.write("top_language - " + topLanguage);
                    bf.newLine();
                    bf.write("top_category - " + topCategory);
                    bf.newLine();
                    bf.write("most_recent_article - " + articles.get(0).getPublished() +
                            " " + articles.get(0).getUrl());
                    bf.newLine();
                    bf.write("top_keyword_en - " + countWords.get(0).word + " " + countWords.get(0).count);
                    bf.newLine();
                    bf.flush();
                    bf.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (assignedArray.equals("key_words")) {
                try {
                    BufferedWriter bf = new BufferedWriter(new FileWriter("keywords_count.txt"));
                    for (CountWords countWord : countWords) {
                        bf.write(countWord.word + " " + countWord.count);
                        bf.newLine();
                    }
                    bf.flush();
                    bf.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String getFileName(String assignedArray) {
        String result = "";
        for (char ch : assignedArray.toCharArray()) {
            if (ch == ' ')
                result += '_';
            else if (ch == ',') {
                continue;
            } else
                result += ch;
        }
        return result;
    }


    private String getBestOf(ConcurrentHashMap<String, Integer> importantArray, int id) {
        int maxSomething = -1;
        String bestSomething = "";
        for (Map.Entry<String, Integer> entry : importantArray.entrySet()) {
            String author = entry.getKey();
            int count = entry.getValue();

            if (count > maxSomething) {
                maxSomething = count;
                bestSomething = author;
            } else if (count == maxSomething) {
                if (author.compareTo(bestSomething) < 0) {
                    bestSomething = author;
                }
            }
        }
        if (id == 1) {
            bestSomething = getFileName(bestSomething);
        }
        bestSomething += " " + maxSomething;
        return bestSomething;
    }

    public void combineLocalToShared(HashMap<String, Integer> local,
                                     ConcurrentHashMap<String, Integer> shared) {
        for (Map.Entry<String, Integer> entry : local.entrySet()) {
            String key =  entry.getKey();
            Integer value = entry.getValue();
            shared.merge(key, value, Integer::sum);
        }

    }
}
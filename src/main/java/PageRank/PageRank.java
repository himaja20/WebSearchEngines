package PageRank;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/*
 * TODO
 * Implementation of comparator of pages based on scores
 * Jcommander arguments
 * Cleaning up of code
 */


public class PageRank {

  public static void main(String[] args) throws IOException {
    PageRank pr = new PageRank();
    pr.rank();
  }

  private void rank() throws IOException{

    Parser parser = new Parser();
    ArrayList<Page> pages = new ArrayList<Page>();
    Map<String, Integer> outlinks = new HashMap<String,Integer>();
    Map<String,Page> linkToPage = new HashMap<String,Page>();

    File folder = new File("D:\\JAVA Workspace\\WebSearch\\src\\main\\Resources\\docs\\cs.nyu.edu\\courses\\spring16\\CSCI-GA.2580-001\\Prog3Example\\");
    File[] listOfFiles = folder.listFiles();
    Weights weightObj = new Weights(listOfFiles.length);

    for (File file : listOfFiles) {
      if (file.isFile()) {
        int wordCount = parser.getWordCount(file.getAbsolutePath());
        System.out.println(file.getName() + " " + wordCount);
        Page page = new Page(wordCount,file.getAbsolutePath());
        pages.add(page);
        linkToPage.put(file.getName(), page);
      }
    }
    int baseSum = 0;
    for (int i = 0; i <pages.size();i++){
      baseSum += pages.get(i).getBase();
    }

    for (Page page: pages){
      page.setBase(page.getBase()/baseSum);
      page.setScore(page.getBase());
    }

    for (int i = 0; i <pages.size();i++){
      Page p = pages.get(i);
      if (!parser.hasOutlinks(p.getFileLocation())){
        for(Page q: pages){
          weightObj.setWeights(q,p,q.getScore());
        }
      }
      else{
        outlinks = parser.getOutlinks(p.getFileLocation());
        for (Entry<String, Integer> entry : outlinks.entrySet())
        {
          Page q = linkToPage.get(entry.getKey());
          float weight = entry.getValue();
          weightObj.setWeights(q, p, weight);
        }

        int sum = 0;
        for (Entry<String, Integer> entry : outlinks.entrySet())
        {
          Page q = linkToPage.get(entry.getKey());
          sum += weightObj.getWeights(q, p);
        }

        for (Entry<String, Integer> entry : outlinks.entrySet())
        {
          Page q = linkToPage.get(entry.getKey());
          weightObj.setWeights(q, p, weightObj.getWeights(q, p)/sum);
        }
      }
    }
    float f = (float) 0.7;
    //float change = (float)Integer.MAX_VALUE;
    boolean changed = true;
    float epsilon = (float) (0.01/pages.size());
    
    
    while(changed){
      changed = false;
      
      for (Page pPage: pages){
        float normalizedScore = 0;
        for (Page qPage:pages){
          normalizedScore += (qPage.getScore() * weightObj.getWeights(pPage, qPage));
        }
        float newScore = ((1-f) * pPage.getBase()) + (f * normalizedScore);
        pPage.setNewScore(newScore);
        
        if (Math.abs(pPage.getNewScore()- pPage.getScore()) > epsilon){
          changed = true;
        }
      }
      for (Page pPage: pages){
        pPage.setScore(pPage.getNewScore());
      }
    }
    System.out.println("----------");
    for (Page pPage:pages){
      System.out.println(pPage.getFileName()+" " + pPage.getScore());
    }
  }
}

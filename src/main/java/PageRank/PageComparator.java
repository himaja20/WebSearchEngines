package PageRank;

import java.util.Comparator;

public class PageComparator implements Comparator<Page>{

  @Override
  public int compare(Page p, Page q) {
    if (p.getScore() < q.getScore()){
      return 1;
    }
    
    return -1;
  }

}

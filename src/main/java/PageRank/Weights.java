package PageRank;

public class Weights {
  private float[][] weights;

  public Weights(int numPages){
    weights = new float[numPages][numPages];
  }

  public void setWeights(Page q, Page p, float f){
    weights[q.getId()][p.getId()] = f;
  }
  
  public float getWeights(Page q, Page p){
    return this.weights[q.getId()][p.getId()];
  }
}

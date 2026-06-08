//package algorithm.operators;
//
//
//public interface BaseOperator {
//    double getWeight();
//    void updateWeight(double newWeight);
//    void updateScore(int score);
//    void resetScore();
//    String getName();
//    int getScore();
//    int getUsageCount(); // 抽象方法，所有算子必须实现
//}
package algorithm.operators;

public interface BaseOperator {
    String getName();
    double getWeight();
    void updateWeight(double newWeight);
    int getScore();
    void updateScore(int score);
    void resetScore();
    int getUsageCount();
    void incrementUsageCount();  // 应该在这里
    void resetUsageCount();      // 应该在这里
}
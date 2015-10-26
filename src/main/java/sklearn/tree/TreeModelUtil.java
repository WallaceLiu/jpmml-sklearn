/*
 * Copyright (c) 2015 Villu Ruusmann
 *
 * This file is part of JPMML-SkLearn
 *
 * JPMML-SkLearn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SkLearn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SkLearn.  If not, see <http://www.gnu.org/licenses/>.
 */
package sklearn.tree;

import java.util.List;

import org.dmg.pmml.DataField;
import org.dmg.pmml.MiningFunctionType;
import org.dmg.pmml.MiningSchema;
import org.dmg.pmml.Node;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.ScoreDistribution;
import org.dmg.pmml.SimplePredicate;
import org.dmg.pmml.TreeModel;
import org.dmg.pmml.TreeModel.SplitCharacteristic;
import org.dmg.pmml.True;
import org.dmg.pmml.Value;
import org.jpmml.converter.FieldCollector;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.TreeModelFieldCollector;
import sklearn.Estimator;
import sklearn.EstimatorUtil;

public class TreeModelUtil {

	private TreeModelUtil(){
	}

	static
	public <E extends Estimator & HasTree> TreeModel encodeTreeModel(E estimator, MiningFunctionType miningFunction, List<DataField> dataFields, boolean standalone){
		Tree tree = estimator.getTree();

		int[] leftChildren = tree.getChildrenLeft();
		int[] rightChildren = tree.getChildrenRight();
		int[] features = tree.getFeature();
		double[] thresholds = tree.getThreshold();
		double[] values = tree.getValues();

		Node root = new Node()
			.setId("1")
			.setPredicate(new True());

		encodeNode(root, 0, leftChildren, rightChildren, features, thresholds, values, miningFunction, dataFields);

		FieldCollector fieldCollector = new TreeModelFieldCollector();
		fieldCollector.applyTo(root);

		MiningSchema miningSchema = EstimatorUtil.encodeMiningSchema(dataFields, fieldCollector, standalone);

		TreeModel treeModel = new TreeModel(miningFunction, miningSchema, root)
			.setSplitCharacteristic(SplitCharacteristic.BINARY_SPLIT);

		return treeModel;
	}

	static
	private void encodeNode(Node node, int index, int[] leftChildren, int[] rightChildren, int[] features, double[] thresholds, double[] values, MiningFunctionType miningFunction, List<DataField> dataFields){
		int feature = features[index];

		// A non-leaf (binary split) node
		if(feature >= 0){
			DataField dataField = dataFields.get(feature + 1);

			double threshold = thresholds[index];

			String value = PMMLUtil.formatValue(threshold);

			Predicate leftPredicate = new SimplePredicate(dataField.getName(), SimplePredicate.Operator.LESS_OR_EQUAL)
				.setValue(value);

			Predicate rightPredicate = new SimplePredicate(dataField.getName(), SimplePredicate.Operator.GREATER_THAN)
				.setValue(value);

			int leftIndex = leftChildren[index];
			int rightIndex = rightChildren[index];

			Node leftChild = new Node()
				.setId(String.valueOf(leftIndex + 1))
				.setPredicate(leftPredicate);

			encodeNode(leftChild, leftIndex, leftChildren, rightChildren, features, thresholds, values, miningFunction, dataFields);

			Node rightChild = new Node()
				.setId(String.valueOf(rightIndex + 1))
				.setPredicate(rightPredicate);

			encodeNode(rightChild, rightIndex, leftChildren, rightChildren, features, thresholds, values, miningFunction, dataFields);

			node.addNodes(leftChild, rightChild);
		} else

		// A leaf node
		{
			DataField dataField = dataFields.get(0);

			if((MiningFunctionType.CLASSIFICATION).equals(miningFunction)){
				List<Value> validValues = dataField.getValues();

				double[] scoreRecordCounts = getRow(values, leftChildren.length, validValues.size(), index);

				double recordCount = 0;

				for(double scoreRecordCount : scoreRecordCounts){
					recordCount += scoreRecordCount;
				}

				node.setRecordCount(recordCount);

				String score = null;

				Double probability = null;

				for(int i = 0; i < validValues.size(); i++){
					Value validValue = validValues.get(i);

					ScoreDistribution scoreDistribution = new ScoreDistribution(validValue.getValue(), scoreRecordCounts[i]);

					node.addScoreDistributions(scoreDistribution);

					double scoreProbability = (scoreRecordCounts[i] / recordCount);

					if(probability == null || probability.compareTo(scoreProbability) < 0){
						score = scoreDistribution.getValue();

						probability = scoreProbability;
					}
				}

				node.setScore(score);
			} else

			if((MiningFunctionType.REGRESSION).equals(miningFunction)){
				String score = PMMLUtil.formatValue(values[index]);

				node.setScore(score);
			} else

			{
				throw new IllegalArgumentException();
			}
		}
	}

	static
	private double[] getRow(double[] values, int rows, int columns, int row){

		if(values.length != (rows * columns)){
			throw new IllegalArgumentException();
		}

		double[] result = new double[columns];

		System.arraycopy(values, (row * columns), result, 0, columns);

		return result;
	}
}
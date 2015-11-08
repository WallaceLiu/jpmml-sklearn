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
package sklearn.decomposition;

import java.util.List;

import numpy.core.NDArrayUtil;
import org.dmg.pmml.Apply;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.sklearn.ClassDictUtil;
import sklearn.ManyToManyTransformer;

public class PCA extends ManyToManyTransformer {

	public PCA(String module, String name){
		super(module, name);
	}

	@Override
	public int getNumberOfInputs(){
		int[] shape = getComponentsShape();

		if(shape.length != 2){
			throw new IllegalArgumentException();
		}

		return shape[1];
	}

	@Override
	public int getNumberOfOutputs(){
		int[] shape = getComponentsShape();

		if(shape.length != 2){
			throw new IllegalArgumentException();
		}

		return shape[0];
	}

	@Override
	public Expression encode(int index, List<FieldName> names){
		int[] shape = getComponentsShape();

		int numberOfComponents = shape[0];
		int numberOfFeatures = shape[1];

		if(numberOfFeatures != names.size()){
			throw new IllegalArgumentException();
		}

		List<? extends Number> components = getComponents();
		List<? extends Number> mean = getMean();

		if(getWhiten()){
			throw new IllegalArgumentException();
		}

		List<? extends Number> component = NDArrayUtil.getRow(components, numberOfComponents, numberOfFeatures, index);

		Apply apply = new Apply("sum");

		for(int i = 0; i < numberOfFeatures; i++){
			FieldName name = names.get(i);

			// "($name[i] - mean[i]) * component[i]"
			Expression expression = PMMLUtil.createApply("*", PMMLUtil.createApply("-", new FieldRef(name), PMMLUtil.createConstant(mean.get(i))), PMMLUtil.createConstant(component.get(i)));

			apply.addExpressions(expression);
		}

		return apply;
	}

	public Boolean getWhiten(){
		return (Boolean)get("whiten");
	}

	public List<? extends Number> getComponents(){
		return (List)ClassDictUtil.getArray(this, "components_");
	}

	public List<? extends Number> getMean(){
		return (List)ClassDictUtil.getArray(this, "mean_");
	}

	private int[] getComponentsShape(){
		return ClassDictUtil.getShape(this, "components_");
	}
}
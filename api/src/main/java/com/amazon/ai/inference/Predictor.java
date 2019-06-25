/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazon.ai.inference;

import com.amazon.ai.Context;
import com.amazon.ai.Model;
import com.amazon.ai.TranslateException;
import com.amazon.ai.Translator;
import com.amazon.ai.engine.Engine;
import com.amazon.ai.metric.Metrics;

/**
 * The <code>Predictor</code> interface provides model inference functionality.
 *
 * <p>Users can use this to do inference with {@link Model} with {@link Translator} specified.
 * Following is example code that uses <code>Predictor</code>:
 *
 * <pre>
 * Model model = Model.loadModel(modelDir, modelName);
 *
 * // User must implement Translator interface, read Translator for detail.
 * Translator translator = new MyTranslator();
 *
 * try (Predictor&lt;String, String&gt; predictor = <b>Predictor.newInstance</b>(model, translator)) {
 *   String result = predictor.<b>predict</b>("What's up");
 * }
 * </pre>
 *
 * @see Model
 * @see Translator
 */
public interface Predictor<I, O> extends AutoCloseable {

    /**
     * Create new Predictor based on the model given.
     *
     * @param model The model used for inference
     * @param translator The Object used for preprocessing and post processing
     * @param <I> Input object for preprocessing
     * @param <O> Output object come from postprocessing
     * @return instance of <code>Predictor</code>
     */
    static <I, O> Predictor<I, O> newInstance(Model model, Translator<I, O> translator) {
        return newInstance(model, translator, Context.defaultContext());
    }

    /**
     * Create new Predictor based on the model given.
     *
     * @param model the model used for inference
     * @param translator The Object used for preprocessing and post processing
     * @param context context used for the inference
     * @param <I> Input object for preprocessing
     * @param <O> Output object come from postprocessing
     * @return new instance of <code>Predictor</code>
     */
    static <I, O> Predictor<I, O> newInstance(
            Model model, Translator<I, O> translator, Context context) {
        return Engine.getInstance().newPredictor(model, translator, context);
    }

    /**
     * Predict method used for inference.
     *
     * @param input Input follows the inputObject
     * @return The Output object defined by user
     * @throws TranslateException if an error occurs during prediction
     */
    O predict(I input) throws TranslateException;

    /**
     * Attach a Metrics param to use for benchmark.
     *
     * @param metrics the Metrics class
     */
    void setMetrics(Metrics metrics);

    /** {@inheritDoc} */
    @Override
    void close();
}

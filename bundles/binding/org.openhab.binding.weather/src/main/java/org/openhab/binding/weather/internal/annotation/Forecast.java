/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.weather.internal.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.openhab.binding.weather.internal.model.ProviderName;

/**
 * This annotation marks the provider data structure for forecasts.
 *
 * @author Gerhard Riegler
 * @since 1.6.0
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface Forecast {

    public ProviderName provider();

    public String property();

}

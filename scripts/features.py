# -*- coding: utf-8 -*-
"""
Created on Tue Sep 27 13:08:49 2016

@author: cs390mb

This file is used for extracting features over windows of tri-axial accelerometer
data. We recommend using helper functions like _compute_mean_features(window) to
extract individual features.

As a side note, the underscore at the beginning of a function is a Python
convention indicating that the function has private access (although in reality
it is still publicly accessible).

"""

import numpy as np
import matplotlib.pyplot as plt

def _compute_mean_features(window):
    """
    Computes the mean x, y and z acceleration over the given window.
    """
    mean = np.mean(window, axis=0)
    return mean

def _compute_var_features(window):
    """
    Computes the mean x, y and z acceleration over the given window.
    """
    var = np.var(window, axis=0)
    return var

def _compute_median_features(window):
    """
    Computes the mean x, y and z acceleration over the given window.
    """
    median = np.median(window, axis=0)
    return median

def extract_features(window):
    """
    Here is where you will extract your features from the data over
    the given window. We have given you an example of computing
    the mean and appending it to the feature matrix X.

    Make sure that X is an N x d matrix, where N is the number
    of data points and d is the number of features.

    """

    x = []
    # x = np.append(x, _compute_var_features(window))
    x = np.append(x, _compute_mean_features(window))
    #print x
    x = np.append(x, _compute_var_features(window))
    '''
    mean = _compute_mean_features(window)
    median = _compute_median_features(window)
    var = _compute_var_features(window)
    '''
    '''
    x = np.append(x, _compute_mean_features(window), axis=0)
    x = np.append(x, _compute_var_features(window), axis=0)
    x = np.append(x, _compute_median_features(window), axis=0)
    '''
    '''
    x = np.array([mean, median, var]);

    print window
    print x

    plt.figure(1) # always call plt.figure() unless you want to plot points on an existing plot
    plt.plot(mean, median, 'ro')
    plt.show()
    '''

    return x

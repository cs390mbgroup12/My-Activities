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

# def _compute_var_features(window):
#     """
#     Computes the mean x, y and z acceleration over the given window.
#     """
#     var = np.var(window, axis=0)
#     return var

def _compute_median_features(window):
    """
    Computes the mean x, y and z acceleration over the given window.
    """
    median = np.median(window, axis=0)
    return median

def _compute_mean_magnitude_features(window):
    """
    Computes the magnitude x, y and z acceleration over the given window.
    """
    sq = np.square(window)
    # print sq
    summy = np.sum(sq, axis=1)
    # print summy
    sqry = np.sqrt(summy)
    # print sqry
    avvie = np.average(sqry)
    return avvie

# def _compute_rfft_features(window):
#     t = np.arange(256)
#     s = np.sin(t)
#     n_freq=32
#     sp = np.fft.fft(s, n=n_freq)
#     freq = np.fft.fftfreq(n_freq)
#     freq[sp.argmax()]

def _compute_std_features(window):
    return np.std(window,axis=0)

def  _compute_std_magnitude_features(window):
    sq = np.square(window)
    summy = np.sum(sq, axis=1)
    sqry = np.sqrt(summy)
    return np.std(sqry,axis=0)

def _computer_zero_crossings_features(window):
    x=0
    y=0
    z=0
    meanie=_compute_mean_features(window)
    subbie = np.subtract(window, meanie)
    prev = subbie[0]
    for number in subbie:
        if np.array_equal(np.sign(number),np.sign(prev)) is True:
            continue
        else:
            if np.array_equal(np.sign(number[0]),np.sign(prev[0])) is False:
                x = x+1
                prev[0]=number[0]
            if np.array_equal(np.sign(number[1]),np.sign(prev[1])) is False:
                y = y+1
                prev[1]=number[1]
            if np.array_equal(np.sign(number[2]),np.sign(prev[2])) is False:
                z = z+1
                prev[2]=number[2]
    return [x,y,z]



def extract_features(window):
    """
    Here is where you will extract your features from the data over
    the given window. We have given you an example of computing
    the mean and appending it to the feature matrix X.

    Make sure that X is an N x d matrix, where N is the number
    of data points and d is the number of features.

    """
   # print window
    x = []
    #GRAPH 1 STD_MAGNITUDE VS MEDIAN Z
    x = np.append(x,  _compute_std_magnitude_features(window))
    x = np.append(x, _compute_median_features(window))

    #GRAPH 2 MEAN X VS STD X
    # x = np.append(x,  _compute_mean_features(window))
    # x = np.append(x, _compute_std_features(window))

    # #GRAPH 3 MEAN_MAGNITUDE VS ZERO_CROSSINGS Z
    # x = np.append(x,  _compute_mean_magnitude_features(window))
    # x = np.append(x, _computer_zero_crossings_features(window))

    return x

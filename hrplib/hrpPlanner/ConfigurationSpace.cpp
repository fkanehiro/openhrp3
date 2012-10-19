// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "Configuration.h"
#include "ConfigurationSpace.h"
#define _USE_MATH_DEFINES // for MSVC
#include <math.h>
#include <stdlib.h>

using namespace PathEngine;

ConfigurationSpace::ConfigurationSpace(unsigned int i_size) : m_size(i_size)
{
    m_weights.resize(m_size);
    m_ubounds.resize(m_size);
    m_lbounds.resize(m_size);
    m_isUnboundedRotation.resize(m_size);
    
    for (unsigned int i=0; i<m_size; i++){
        m_weights[i] = 1.0;
        m_ubounds[i] = m_lbounds[i] = 0.0;
        m_isUnboundedRotation[i] = false;
    }
}

bool ConfigurationSpace::isValid(const Configuration &cfg) const
{
    if (m_size != cfg.size()) return false;

    for (unsigned int i=0; i<m_size; i++){
        if (!weight(i)) continue;
        double v = cfg[i];
        if (!m_isUnboundedRotation[i]){
            if (v > m_ubounds[i] || v < m_lbounds[i]) return false;
        }
    }
    return true;
}

double &ConfigurationSpace::weight(unsigned int i_rank)
{
    return m_weights[i_rank];
}

const double ConfigurationSpace::weight(unsigned int i_rank) const
{
    return m_weights[i_rank];
}

void ConfigurationSpace::bounds(unsigned int i_rank, double min, double max)
{
    m_ubounds[i_rank] = max;
    m_lbounds[i_rank] = min;
}

double& ConfigurationSpace::lbound(unsigned int i_rank)
{
    return m_lbounds[i_rank];
}

double& ConfigurationSpace::ubound(unsigned int i_rank)
{
    return m_ubounds[i_rank];
}

Configuration ConfigurationSpace::random()
{
    Configuration cfg(m_size);
    for (unsigned int i=0; i<m_size; i++){
        if (m_isUnboundedRotation[i]){
            cfg.value(i) = (rand()/(double)RAND_MAX) * 2 * M_PI;
        }else{
            double delta = m_ubounds[i] - m_lbounds[i];
            cfg.value(i) = (rand()/(double)RAND_MAX) * delta + m_lbounds[i];
        }
    }
    return cfg;
}

unsigned int ConfigurationSpace::size()
{
    return m_size;
}

void ConfigurationSpace::unboundedRotation(unsigned int i_rank, bool i_flag)
{
    m_isUnboundedRotation[i_rank] = i_flag;
}

bool ConfigurationSpace::unboundedRotation(unsigned int i_rank)
{
    return m_isUnboundedRotation[i_rank];
}

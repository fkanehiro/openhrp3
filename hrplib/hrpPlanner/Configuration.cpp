// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#include "Configuration.h"
#define _USE_MATH_DEFINES // for MSVC
#include <math.h>
#include <stdlib.h>

namespace PathEngine{
    std::ostream& operator<< (std::ostream& out, const Configuration& cfg)
    {
        for (unsigned int i=0; i<cfg.size(); i++){
            out << cfg.value(i) << " ";
        }
        return out;
    }
}

using namespace PathEngine;

unsigned int Configuration::m_size=0;
std::vector<double> Configuration::m_weights;
std::vector<double> Configuration::m_ubounds;
std::vector<double> Configuration::m_lbounds;
std::vector<bool> Configuration::m_isUnboundedRotation;

Configuration::Configuration()
{
    if (size()==0){
        std::cerr << "Configuration is created before its size is set"
                  << std::endl;
    }
    m_values.resize(size());
    for (unsigned int i=0; i<size(); i++){
        m_values[i] = 0.0;
    }
}
bool Configuration::isValid() const
{
    for (unsigned int i=0; i<size(); i++){
        if (!weight(i)) continue;
        double v = m_values[i];
        if (!m_isUnboundedRotation[i]){
            if (v > m_ubounds[i] || v < m_lbounds[i]) return false;
        }
    }
    return true;
}

double &Configuration::weight(unsigned int i_rank)
{
    return m_weights[i_rank];
}

void Configuration::bounds(unsigned int i_rank, double min, double max)
{
    m_ubounds[i_rank] = max;
    m_lbounds[i_rank] = min;
}

double& Configuration::lbound(unsigned int i_rank)
{
    return m_lbounds[i_rank];
}

double& Configuration::ubound(unsigned int i_rank)
{
    return m_ubounds[i_rank];
}

Configuration Configuration::random()
{
    Configuration cfg;
    for (unsigned int i=0; i<size(); i++){
        if (m_isUnboundedRotation[i]){
            cfg.value(i) = (rand()/(double)RAND_MAX) * 2 * M_PI;
        }else{
            double delta = m_ubounds[i] - m_lbounds[i];
            cfg.value(i) = (rand()/(double)RAND_MAX) * delta + m_lbounds[i];
        }
    }
    return cfg;
}

const double Configuration::value(unsigned int i_rank) const
{
    return m_values[i_rank];
}

double &Configuration::value(unsigned int i_rank)
{
    return m_values[i_rank];
}

unsigned int Configuration::size()
{
    return m_size;
}

void Configuration::size(unsigned int i_size)
{
    m_size = i_size;
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

void Configuration::unboundedRotation(unsigned int i_rank, bool i_flag)
{
    m_isUnboundedRotation[i_rank] = i_flag;
}

bool Configuration::unboundedRotation(unsigned int i_rank)
{
    return m_isUnboundedRotation[i_rank];
}

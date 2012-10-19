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

Configuration::Configuration(unsigned int i_size)
{
    m_values.resize(i_size);
}

const double Configuration::value(unsigned int i_rank) const
{
    return m_values[i_rank];
}

double &Configuration::value(unsigned int i_rank)
{
    return m_values[i_rank];
}

unsigned int Configuration::size() const
{
    return m_values.size();
}

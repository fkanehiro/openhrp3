// -*- mode: c++; indent-tabs-mode: nil; c-basic-offset: 4; tab-width: 4; -*-
#ifndef __CONFIGURATION_H__
#define __CONFIGURATION_H__

#include "exportdef.h"
#include <vector>
#include <iostream>

namespace PathEngine {
    class HRPPLANNER_API Configuration
    {
    public:
        /**
           @brief constructor
        */
        Configuration(unsigned int i_size);

        /**
           @breif get value of \e i_rank th dof
           @param i_rank rank of dof
           @return value of \e i_rank th dof
        */
        const double value(unsigned int i_rank) const;
        const double operator[](unsigned int i_rank) const { return value(i_rank); }

        /**
           @breif set value of \e i_rank th dof
           @param i_rank rank of dof
           @param i_value value of dof
        */
        double& value(unsigned int i_rank);
        double& operator[](unsigned int i_rank) { return value(i_rank); }
    
        /**
           @brief get the number of degrees of freedom
           @return the number of degrees of freedom
         */
        unsigned int size() const;

    private:    
        std::vector<double> m_values;
    };

std::ostream& operator<< (std::ostream& out, const PathEngine::Configuration& cfg);
};


#endif

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
        Configuration();

        /**
         * @brief x,y,thetaのそれぞれが有効な範囲内にあるかどうか検査する
         * @return 全ての要素が有効範囲内にあればtrue、それ以外false
         */
        bool isValid() const;

        /**
         * @brief set weight for X element
         * @param w weight
         */
        static double &weight(unsigned int i_rank);

        /**
         * @brief set bounds for Y element
         * @param min minimum value
         * @param max maximum value
         */
        static void bounds(unsigned int i_rank, double min, double max);

        /**
         * @brief get bounds for Y element
         * @return maximum value
         */
        static double& ubound(unsigned int i_rank);

        /**
         * @brief get lower bound of \e i_rank the value
         * @return lower bound
         */
        static double& lbound(unsigned int i_rank);

        /**
           @brief specify \e i th degree of freedom is unbounded rotaion or not. default is false
           @param i_rank rank of the degree of freedom
           @param i_flag true or false
         */
        static void unboundedRotation(unsigned int i_rank, bool i_flag);

        /**
           @brief ask \e i the degree of freedom is unbounded rotaion or not
           @param i_rank rank of the degree of freedom
           @return true(unbounded) or false
         */
        static bool unboundedRotation(unsigned int i_rank);

        /**
         * @brief generate random position
         * @return generated position
         */
        static Configuration random();

        /**
           @breif get value of \e i_rank th dof
           @param i_rank rank of dof
           @return value of \e i_rank th dof
        */
        const double value(unsigned int i_rank) const;

        /**
           @breif set value of \e i_rank th dof
           @param i_rank rank of dof
           @param i_value value of dof
        */
        double& value(unsigned int i_rank);
    
        /**
           @brief get the number of degrees of freedom
           @return the number of degrees of freedom
         */
        static unsigned int size();

        /**
           @brief set the number of degrees of freedom
           @i_size the number of degrees of freedom
         */
        static void size(unsigned int i_size);

    private:    
        std::vector<double> m_values;
        static unsigned int m_size;
        static std::vector<double> m_ubounds;
        static std::vector<double> m_lbounds;
        static std::vector<double> m_weights;
        static std::vector<bool> m_isUnboundedRotation;
    };

std::ostream& operator<< (std::ostream& out, const PathEngine::Configuration& cfg);
};


#endif

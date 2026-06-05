import { Box, Card, CardContent, Typography } from '@mui/material'
import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router';
import { getActivities } from '../services/api';

const ActivityList = () => {
    const [activities, setActivities] = useState([]);
    const navigate = useNavigate();

    const fetchActivities = async () => {
        try {
            const response = await getActivities();
            setActivities(response.data);
        } catch (error) {
            console.error(error);
        }
    };

    useEffect(() => {
        fetchActivities();
    }, []);

    return (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2 }}>
            {activities.map((activity) => (
                <Card
                    key={activity.id}
                    sx={{ cursor: 'pointer', minWidth: 220, flex: '1 1 220px' }}
                    onClick={() => navigate(`/activities/${activity.id}`)}
                >
                    <CardContent>
                        <Typography variant='h6'>{activity.type}</Typography>
                        <Typography>Duration: {activity.duration} min</Typography>
                        <Typography>Calories: {activity.caloriesBurned}</Typography>
                    </CardContent>
                </Card>
            ))}
        </Box>
    )
}

export default ActivityList
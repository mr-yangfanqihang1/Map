const express = require('express');
const mysql = require('mysql2');
const bodyParser = require('body-parser');
const cors = require('cors');
require('dotenv').config(); // 使用环境变量

// 创建 Express 应用
const app = express();
const PORT = process.env.PORT || 8081;

// 中间件设置
app.use(cors());
app.use(bodyParser.json());

// 创建 MySQL 连接池
const db = mysql.createPool({
    host: 'localhost',
    user: process.env.DB_USER || '扬帆起航',
    password: process.env.DB_PASSWORD || 'yangfanqihang',
    database: process.env.DB_NAME || 'map',
    waitForConnections: true,
    connectionLimit: 10, // 最大连接数
    queueLimit: 0,
});

// 模糊搜索 API
app.get('/api/search', (req, res) => {
    const query = req.query.query;

    if (!query) {
        return res.status(400).json({ message: 'Query parameter is required' });
    }

    // 使用 MySQL 的 LIKE 语句进行模糊搜索，并返回 id 和 name
    const sql = 'SELECT id, name FROM items WHERE name LIKE ?';
    const values = [`%${query}%`];

    db.query(sql, values, (error, results) => {
        if (error) {
            console.error('Error fetching data:', error);
            return res.status(500).json({ message: 'Server error' });
        }
        res.json(results); // 返回结果包含 id 和 name
    });
});

// 启动服务器
app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});

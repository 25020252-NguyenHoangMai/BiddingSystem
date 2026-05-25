USE [master]
GO
/****** Object:  Database [BiddingSystem]    Script Date: 5/25/2026 5:10:38 PM ******/
CREATE DATABASE [BiddingSystem]
 CONTAINMENT = NONE
 ON  PRIMARY
( NAME = N'BiddingSystem', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\DATA\BiddingSystem.mdf' , SIZE = 73728KB , MAXSIZE = UNLIMITED, FILEGROWTH = 65536KB )
 LOG ON
( NAME = N'BiddingSystem_log', FILENAME = N'C:\Program Files\Microsoft SQL Server\MSSQL16.SQLEXPRESS\MSSQL\DATA\BiddingSystem_log.ldf' , SIZE = 8192KB , MAXSIZE = 2048GB , FILEGROWTH = 65536KB )
 WITH CATALOG_COLLATION = DATABASE_DEFAULT, LEDGER = OFF
GO
ALTER DATABASE [BiddingSystem] SET COMPATIBILITY_LEVEL = 160
GO
IF (1 = FULLTEXTSERVICEPROPERTY('IsFullTextInstalled'))
begin
EXEC [BiddingSystem].[dbo].[sp_fulltext_database] @action = 'enable'
end
GO
ALTER DATABASE [BiddingSystem] SET ANSI_NULL_DEFAULT OFF
GO
ALTER DATABASE [BiddingSystem] SET ANSI_NULLS OFF
GO
ALTER DATABASE [BiddingSystem] SET ANSI_PADDING OFF
GO
ALTER DATABASE [BiddingSystem] SET ANSI_WARNINGS OFF
GO
ALTER DATABASE [BiddingSystem] SET ARITHABORT OFF
GO
ALTER DATABASE [BiddingSystem] SET AUTO_CLOSE OFF
GO
ALTER DATABASE [BiddingSystem] SET AUTO_SHRINK OFF
GO
ALTER DATABASE [BiddingSystem] SET AUTO_UPDATE_STATISTICS ON
GO
ALTER DATABASE [BiddingSystem] SET CURSOR_CLOSE_ON_COMMIT OFF
GO
ALTER DATABASE [BiddingSystem] SET CURSOR_DEFAULT  GLOBAL
GO
ALTER DATABASE [BiddingSystem] SET CONCAT_NULL_YIELDS_NULL OFF
GO
ALTER DATABASE [BiddingSystem] SET NUMERIC_ROUNDABORT OFF
GO
ALTER DATABASE [BiddingSystem] SET QUOTED_IDENTIFIER OFF
GO
ALTER DATABASE [BiddingSystem] SET RECURSIVE_TRIGGERS OFF
GO
ALTER DATABASE [BiddingSystem] SET  ENABLE_BROKER
GO
ALTER DATABASE [BiddingSystem] SET AUTO_UPDATE_STATISTICS_ASYNC OFF
GO
ALTER DATABASE [BiddingSystem] SET DATE_CORRELATION_OPTIMIZATION OFF
GO
ALTER DATABASE [BiddingSystem] SET TRUSTWORTHY OFF
GO
ALTER DATABASE [BiddingSystem] SET ALLOW_SNAPSHOT_ISOLATION OFF
GO
ALTER DATABASE [BiddingSystem] SET PARAMETERIZATION SIMPLE
GO
ALTER DATABASE [BiddingSystem] SET READ_COMMITTED_SNAPSHOT OFF
GO
ALTER DATABASE [BiddingSystem] SET HONOR_BROKER_PRIORITY OFF
GO
ALTER DATABASE [BiddingSystem] SET RECOVERY SIMPLE
GO
ALTER DATABASE [BiddingSystem] SET  MULTI_USER
GO
ALTER DATABASE [BiddingSystem] SET PAGE_VERIFY CHECKSUM
GO
ALTER DATABASE [BiddingSystem] SET DB_CHAINING OFF
GO
ALTER DATABASE [BiddingSystem] SET FILESTREAM( NON_TRANSACTED_ACCESS = OFF )
GO
ALTER DATABASE [BiddingSystem] SET TARGET_RECOVERY_TIME = 60 SECONDS
GO
ALTER DATABASE [BiddingSystem] SET DELAYED_DURABILITY = DISABLED
GO
ALTER DATABASE [BiddingSystem] SET ACCELERATED_DATABASE_RECOVERY = OFF
GO
ALTER DATABASE [BiddingSystem] SET QUERY_STORE = ON
GO
ALTER DATABASE [BiddingSystem] SET QUERY_STORE (OPERATION_MODE = READ_WRITE, CLEANUP_POLICY = (STALE_QUERY_THRESHOLD_DAYS = 30), DATA_FLUSH_INTERVAL_SECONDS = 900, INTERVAL_LENGTH_MINUTES = 60, MAX_STORAGE_SIZE_MB = 1000, QUERY_CAPTURE_MODE = AUTO, SIZE_BASED_CLEANUP_MODE = AUTO, MAX_PLANS_PER_QUERY = 200, WAIT_STATS_CAPTURE_MODE = ON)
GO
USE [BiddingSystem]
GO
/****** Object:  Table [dbo].[AuctionSession]    Script Date: 5/25/2026 5:10:39 PM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AuctionSession](
    [id] [nvarchar](50) NOT NULL,
    [itemId] [nvarchar](50) NOT NULL,
    [currentPrice] [decimal](18, 2) NOT NULL,
    [currentWinnerId] [nvarchar](50) NULL,
    [startTime] [datetime2](7) NOT NULL,
    [endTime] [datetime2](7) NOT NULL,
    [status] [nvarchar](50) NOT NULL,
    CONSTRAINT [PK_AuctionSession] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[AutoBid]    Script Date: 5/25/2026 5:10:39 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[AutoBid](
    [id] [nvarchar](50) NOT NULL,
    [sessionId] [nvarchar](50) NOT NULL,
    [bidderId] [nvarchar](50) NOT NULL,
    [maxBidAmount] [money] NOT NULL,
    [isActive] [bit] NOT NULL,
    [createdAt] [datetime2](7) NOT NULL,
    [updatedAt] [datetime2](7) NOT NULL,
    CONSTRAINT [PK_AutoBid_1] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[BidTransaction]    Script Date: 5/25/2026 5:10:39 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[BidTransaction](
    [id] [nvarchar](50) NOT NULL,
    [sessionId] [nvarchar](50) NOT NULL,
    [bidderId] [nvarchar](50) NOT NULL,
    [bidAmount] [decimal](18, 2) NOT NULL,
    [bidTime] [datetime2](7) NOT NULL,
    CONSTRAINT [PK_BidTransaction] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[Item]    Script Date: 5/25/2026 5:10:39 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[Item](
    [id] [nvarchar](50) NOT NULL,
    [name] [nvarchar](max) NOT NULL,
    [description] [nvarchar](max) NULL,
    [itemType] [nvarchar](50) NOT NULL,
    [sellerId] [nvarchar](50) NOT NULL,
    [startingPrice] [money] NULL,
    [model] [nvarchar](max) NULL,
    [engineType] [nvarchar](max) NULL,
    [mileage] [int] NULL,
    [brand] [nvarchar](max) NULL,
    [artist] [nvarchar](max) NULL,
    [imagePath] [nvarchar](500) NULL,
    CONSTRAINT [PK_Item] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
    GO
/****** Object:  Table [dbo].[Users]    Script Date: 5/25/2026 5:10:39 PM ******/
    SET ANSI_NULLS ON
    GO
    SET QUOTED_IDENTIFIER ON
    GO
CREATE TABLE [dbo].[Users](
    [id] [nvarchar](50) NOT NULL,
    [username] [nvarchar](50) NOT NULL,
    [password] [nvarchar](max) NOT NULL,
    [fullName] [nvarchar](max) NOT NULL,
    [balance] [money] NOT NULL,
    [sellerEnabled] [bit] NOT NULL,
    [role] [nvarchar](50) NULL,
    [reservedBalance] [money] NOT NULL,
    [status] [nvarchar](20) NOT NULL,
    [deactivatedAt] [datetime2](7) NULL,
    CONSTRAINT [PK_Users] PRIMARY KEY CLUSTERED
(
[id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY],
    CONSTRAINT [IX_Users] UNIQUE NONCLUSTERED
(
[username] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
    ) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
    GO
    SET ANSI_PADDING ON
    GO
/****** Object:  Index [IX_AutoBid]    Script Date: 5/25/2026 5:10:39 PM ******/
CREATE UNIQUE NONCLUSTERED INDEX [IX_AutoBid] ON [dbo].[AutoBid]
(
	[bidderId] ASC,
	[sessionId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[AuctionSession] ADD  CONSTRAINT [DF_AuctionSession_currentPrice]  DEFAULT ((0)) FOR [currentPrice]
    GO
ALTER TABLE [dbo].[AutoBid] ADD  CONSTRAINT [DF_AutoBid_isActive]  DEFAULT ((1)) FOR [isActive]
    GO
ALTER TABLE [dbo].[BidTransaction] ADD  CONSTRAINT [DF_BidTransaction_bidTime]  DEFAULT (getdate()) FOR [bidTime]
    GO
ALTER TABLE [dbo].[Item] ADD  CONSTRAINT [DF_Item_startingPrice]  DEFAULT ((0)) FOR [startingPrice]
    GO
ALTER TABLE [dbo].[Users] ADD  CONSTRAINT [DF_Users_balance]  DEFAULT ((0.0)) FOR [balance]
    GO
ALTER TABLE [dbo].[Users] ADD  CONSTRAINT [DF_Users_sellerEnabled]  DEFAULT ((0)) FOR [sellerEnabled]
    GO
ALTER TABLE [dbo].[Users] ADD  CONSTRAINT [DF_Users_reservedBalance]  DEFAULT ((0.0)) FOR [reservedBalance]
    GO
ALTER TABLE [dbo].[Users] ADD  CONSTRAINT [DF_Users_status]  DEFAULT ('ACTIVE') FOR [status]
    GO
ALTER TABLE [dbo].[AuctionSession]  WITH CHECK ADD  CONSTRAINT [FK_AuctionSession_AuctionSession] FOREIGN KEY([itemId])
    REFERENCES [dbo].[Item] ([id])
    GO
ALTER TABLE [dbo].[AuctionSession] CHECK CONSTRAINT [FK_AuctionSession_AuctionSession]
    GO
ALTER TABLE [dbo].[AuctionSession]  WITH CHECK ADD  CONSTRAINT [FK_AuctionSession_Users] FOREIGN KEY([currentWinnerId])
    REFERENCES [dbo].[Users] ([id])
    GO
ALTER TABLE [dbo].[AuctionSession] CHECK CONSTRAINT [FK_AuctionSession_Users]
    GO
ALTER TABLE [dbo].[AutoBid]  WITH CHECK ADD  CONSTRAINT [FK_AutoBid_AuctionSession] FOREIGN KEY([sessionId])
    REFERENCES [dbo].[AuctionSession] ([id])
    GO
ALTER TABLE [dbo].[AutoBid] CHECK CONSTRAINT [FK_AutoBid_AuctionSession]
    GO
ALTER TABLE [dbo].[AutoBid]  WITH CHECK ADD  CONSTRAINT [FK_AutoBid_Users] FOREIGN KEY([bidderId])
    REFERENCES [dbo].[Users] ([id])
    GO
ALTER TABLE [dbo].[AutoBid] CHECK CONSTRAINT [FK_AutoBid_Users]
    GO
ALTER TABLE [dbo].[BidTransaction]  WITH CHECK ADD  CONSTRAINT [FK_BidTransaction_AuctionSession] FOREIGN KEY([sessionId])
    REFERENCES [dbo].[AuctionSession] ([id])
    GO
ALTER TABLE [dbo].[BidTransaction] CHECK CONSTRAINT [FK_BidTransaction_AuctionSession]
    GO
ALTER TABLE [dbo].[BidTransaction]  WITH CHECK ADD  CONSTRAINT [FK_BidTransaction_BidTransaction] FOREIGN KEY([bidderId])
    REFERENCES [dbo].[Users] ([id])
    GO
ALTER TABLE [dbo].[BidTransaction] CHECK CONSTRAINT [FK_BidTransaction_BidTransaction]
    GO
ALTER TABLE [dbo].[Item]  WITH CHECK ADD  CONSTRAINT [FK_Item_Users] FOREIGN KEY([sellerId])
    REFERENCES [dbo].[Users] ([id])
    GO
ALTER TABLE [dbo].[Item] CHECK CONSTRAINT [FK_Item_Users]
    GO
ALTER TABLE [dbo].[AuctionSession]  WITH CHECK ADD  CONSTRAINT [CK_AuctionSession_Status] CHECK  (([status]='CANCELED' OR [status]='FINISHED' OR [status]='RUNNING' OR [status]='OPEN' OR [status]='PAID'))
    GO
ALTER TABLE [dbo].[AuctionSession] CHECK CONSTRAINT [CK_AuctionSession_Status]
    GO
ALTER TABLE [dbo].[AuctionSession]  WITH CHECK ADD  CONSTRAINT [CK_AuctionSession_TimeLogic] CHECK  (([endTime]>[startTime]))
    GO
ALTER TABLE [dbo].[AuctionSession] CHECK CONSTRAINT [CK_AuctionSession_TimeLogic]
    GO
ALTER TABLE [dbo].[Item]  WITH CHECK ADD  CONSTRAINT [CK_Item_Type] CHECK  (([itemType]='VEHICLE' OR [itemType]='ELECTRONICS' OR [itemType]='ART' OR [itemType]='OTHER'))
    GO
ALTER TABLE [dbo].[Item] CHECK CONSTRAINT [CK_Item_Type]
    GO
ALTER TABLE [dbo].[Users]  WITH CHECK ADD  CONSTRAINT [CK_Users_status] CHECK  (([status]='ACTIVE' OR [status]='DISABLED'))
    GO
ALTER TABLE [dbo].[Users] CHECK CONSTRAINT [CK_Users_status]
    GO
    USE [master]
    GO
ALTER DATABASE [BiddingSystem] SET  READ_WRITE
GO

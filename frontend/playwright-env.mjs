import { fileURLToPath } from 'node:url';

process.env.PLAYWRIGHT_BROWSERS_PATH ||= fileURLToPath(new URL('./.playwright-browsers', import.meta.url));
